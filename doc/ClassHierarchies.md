# Class hierarchies

By default, BooPickle encodes zero type information, which makes it impossible to directly encode a class hierarchy like below and decode it
just by specifying the parent type `Fruit`.

```scala
trait Fruit {
  val weight: Double
  def color: String
}

case class Banana(weight: Double) extends Fruit {
  def color = "yellow"
}

case class Kiwi(weight: Double) extends Fruit {
  def color = "brown"
}

case class Carambola(weight: Double) extends Fruit {
  def color = "yellow"
}
```

As this is such a common situation, BooPickle provides a helper class `CompositePickler` to build a custom pickler for composite types. For the case
above, all you need to do is to define an implicit pickler like this, utilizing the `compositePickler` function from `Default`:

```scala
implicit val fruitPickler = compositePickler[Fruit].
  addConcreteType[Banana].
  addConcreteType[Kiwi].
  addConcreteType[Carambola]
```

Now you can freely pickle any `Fruit` and when unpickling, BooPickle will know what type to decode.

```scala
val fruits: Seq[Fruit] = Seq(Kiwi(0.5), Kiwi(0.6), Carambola(5.0), Banana(1.2))
val bb = Pickle.intoBytes(fruits)
.
.
val u = Unpickle[Seq[Fruit]].fromBytes(bb)
assert(u == fruits)
```

Note that internally `CompositePickler` encodes types using indices, so they must be specified in the same order on both sides!

BooPickle needs to know the type when pickling to deserialize to the correct type, thus this fails

```scala
val b = Banana(1.0)
val bb = Pickle.intoBytes(b)
assert(Unpickle[Banana].fromBytes(bb) == b) // This produces Banana
val bb2 = Pickle.intoBytes(b)
assert(Unpickle[Fruit].fromBytes(bb2) == null) // This produces null
```

Instead when pickling declare the parent type

```scala
val f: Fruit = Banana(1.0)
val bf = Pickle.intoBytes(f)
assert(Unpickle[Fruit].fromBytes(bf) == f) // This produces a Fruit
```

### Recursive composite types

If you have a recursive composite type (a sub type has a reference to the super type), you need to build the `CompositePickler` in two steps,
as shown below.

```scala
sealed trait Tree
case object Leaf extends Tree
case class Node(value: Int, children:Seq[Tree]) extends Tree

object Tree {
  implicit val treePickler = compositePickler[Tree]
  treePickler.addConcreteType[Node].addConcreteType[Leaf.type]
}
```

This is because the compiler must find a pickler for `Tree` when it's building a pickler for `Node`.

### Automatic generation of hierarchy picklers

If your type hierarchy is `sealed` then you can take advantage of the automatic pickler generation feature of BooPickle. A macro automatically generates
the required `CompositePickler` for you, as long as the trait is `sealed`. For example lets change the `Fruit` trait to be sealed, so that compiler
knows all its descendants will be defined in the same file and the macro can find them.

```scala
sealed trait Fruit {
  val weight: Double
  def color: String
}
```

Now you can directly pickle your fruits without manually defining a `CompositePickler`.

```scala
val fruits: Seq[Fruit] = Seq(Kiwi(0.5), Kiwi(0.6), Carambola(5.0), Banana(1.2))
val bb = Pickle.intoBytes(fruits)
.
.
val u = Unpickle[Seq[Fruit]].fromBytes(bb)
assert(u == fruits)
```

Note that for some hierarchies the automatic generation may not work (due to Scala compiler limitations), but you can always fall back to the
manually defined `CompositePickler`.

Also note that due to the way macros generate picklers, each time you need an implicit instance of the pickler, new classes (and `.class` files)
will be generated. And not just for the top level trait, but for all implementing classes as well. If you have a large class hierarchy, this adds up
rather quickly! Below you can see the results of pickling a trait twice in the code.

``` 
 Size   Name
 2,798  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$14$TraitPickler$macro$25$2$CCPickler$macro$26$2$.class
 2,798  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$16$TraitPickler$macro$33$2$CCPickler$macro$34$2$.class
 3,498  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$14$TraitPickler$macro$25$2$CCPickler$macro$27$2$.class
 3,498  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$16$TraitPickler$macro$33$2$CCPickler$macro$35$2$.class
 4,789  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$14$TraitPickler$macro$25$2$.class
 4,789  MacroPickleTests$$anonfun$tests$8$$anonfun$apply$1$$anonfun$apply$16$TraitPickler$macro$33$2$.class
``` 

If this becomes an issue, you can avoid it by storing implicit picklers in the companion object of the trait. This way the code is generated only once
and used whenever you need a pickler for your `Fruit`.

```scala
object Fruit {
  implicit val pickler: Pickler[Fruit] = generatePickler[Fruit]
}

// must import the companion object, otherwise the implicit macro has higher precedence and will generate another pickler!
import Fruit._
val fruits: Seq[Fruit] = Seq(Kiwi(0.5), Kiwi(0.6), Carambola(5.0), Banana(1.2))
val bb = Pickle.intoBytes(fruits)
```

You can prevent the implicit use of the pickler generator macro by importing `boopickle.DefaultBasic._` instead of 
`boopickle.Default._` as this will leave the implicit macro code out. Then you can provide specific implicit picklers for your 
case classes or class hierarchies.

```scala
import boopickle.DefaultBasic._
object Fruit {
  // use macro explicitly to generate the pickler
  implicit val pickler: Pickler[Fruit] = generatePickler[Fruit]
}
```

In this case you don't need to `import Fruit._` because there is no implicit macro to compete with your pickler in the companion object.

Note that when not using implicit macro picklers, you must pay special attention to the creation order of picklers in more complex situations like below.

```scala
import boopickle.DefaultBasic._
sealed trait MyTrait

case class TT1(i: Int) extends MyTrait

case class TT2(s: String, next: MyTrait) extends MyTrait

class TT3(val i: Int, val s: String) extends MyTrait

object MyTrait {
  // picklers must be created in correct order, because TT2 depends on MyTrait
  implicit val pickler = compositePickler[MyTrait]
  // use macro explicitly to generate picklers for TT1 and TT2
  implicit val pickler1 = generatePickler[TT1]
  implicit val pickler2 = generatePickler[TT2]
  // a pickler for TT3 cannot be generated by macro, so use a transform pickler
  implicit val pickler3 = transformPickler[TT3, (Int, String)](t => (t.i, t.s), t => new TT3(t._1, t._2))
  pickler.addConcreteType[TT1].addConcreteType[TT2].addConcreteType[TT3]
}
```

### Complex type hierarchies

When you have more complex type hierarchies with multiple levels of traits, you might need picklers for each type level. A simple example to illustrate:

```scala
sealed trait Element

sealed trait Document extends Element

sealed trait Attribute extends Element

final case class WordDocument(text:String) extends Document

final case class OwnerAttribute(owner: String, parent: Element) extends Attribute
```

Building a `CompositePickler` for `Element` with the two implementation classes doesn't actually give you a pickler for `Document` nor `Attribute`. So
you need to define those picklers separately, duplicating the implementation classes. For this purpose `CompositePickler` allows you to join existing
composite picklers to form a new one.

```scala
object Element {
  implicit val documentPickler = compositePickler[Document]
  documentPickler.addConcreteType[WordDocument]

  implicit val attributePickler = compositePickler[Attribute]
  attributePickler.addConcreteType[OwnerAttribute]

  implicit val elementPickler = compositePickler[Element]
  elementPickler.join[Document].join[Attribute]
}
```

With these picklers you may now pickle any trait. Note, however, that you must use the same `CompositePickler` when unpickling. You cannot pickle with `Element` 
and unpickle with `Attribute` even if the actual class was `OwnerAttribute` because internal indexes are different for each composite pickler.

