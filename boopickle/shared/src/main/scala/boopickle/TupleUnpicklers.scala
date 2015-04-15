package boopickle

trait TupleUnpicklers extends UnpicklerHelper {
  
  implicit def Tuple1Unpickler[T1: U] = new U[Tuple1[T1]] {
    override def unpickle(implicit state: UnpickleState) = Tuple1[T1](read[T1])
  }

  implicit def Tuple2Unpickler[T1: U, T2: U] = new U[(T1, T2)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2])
  }

  implicit def Tuple3Unpickler[T1: U, T2: U, T3: U] = new U[(T1, T2, T3)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3])
  }

  implicit def Tuple4Unpickler[T1: U, T2: U, T3: U, T4: U] = new U[(T1, T2, T3, T4)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4])
  }

  implicit def Tuple5Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U] = new U[(T1, T2, T3, T4, T5)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5])
  }

  implicit def Tuple6Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U] = new U[(T1, T2, T3, T4, T5, T6)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6])
  }

  implicit def Tuple7Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U] = new U[(T1, T2, T3, T4, T5, T6, T7)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7])
  }

  implicit def Tuple8Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8])
  }

  implicit def Tuple9Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9])
  }

  implicit def Tuple10Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10])
  }

  implicit def Tuple11Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11])
  }

  implicit def Tuple12Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12])
  }

  implicit def Tuple13Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U, T13: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12], read[T13])
  }

  implicit def Tuple14Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U, T13: U, T14: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12], read[T13], read[T14])
  }

  implicit def Tuple15Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U, T13: U, T14: U, T15: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12], read[T13], read[T14], read[T15])
  }

  implicit def Tuple16Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U, T13: U, T14: U, T15: U, T16: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12], read[T13], read[T14], read[T15], read[T16])
  }

  implicit def Tuple17Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U, T13: U, T14: U, T15: U, T16: U, T17: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12], read[T13], read[T14], read[T15], read[T16], read[T17])
  }

  implicit def Tuple18Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U, T13: U, T14: U, T15: U, T16: U, T17: U, T18: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12], read[T13], read[T14], read[T15], read[T16], read[T17], read[T18])
  }

  implicit def Tuple19Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U, T13: U, T14: U, T15: U, T16: U, T17: U, T18: U, T19: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12], read[T13], read[T14], read[T15], read[T16], read[T17], read[T18], read[T19])
  }

  implicit def Tuple20Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U, T13: U, T14: U, T15: U, T16: U, T17: U, T18: U, T19: U, T20: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12], read[T13], read[T14], read[T15], read[T16], read[T17], read[T18], read[T19], read[T20])
  }

  implicit def Tuple21Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U, T13: U, T14: U, T15: U, T16: U, T17: U, T18: U, T19: U, T20: U, T21: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12], read[T13], read[T14], read[T15], read[T16], read[T17], read[T18], read[T19], read[T20], read[T21])
  }

  implicit def Tuple22Unpickler[T1: U, T2: U, T3: U, T4: U, T5: U, T6: U, T7: U, T8: U, T9: U, T10: U, T11: U, T12: U, T13: U, T14: U, T15: U, T16: U, T17: U, T18: U, T19: U, T20: U, T21: U, T22: U] = new U[(T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, T21, T22)] {
    override def unpickle(implicit state: UnpickleState) = (read[T1], read[T2], read[T3], read[T4], read[T5], read[T6], read[T7], read[T8], read[T9], read[T10], read[T11], read[T12], read[T13], read[T14], read[T15], read[T16], read[T17], read[T18], read[T19], read[T20], read[T21], read[T22])
  }
}
