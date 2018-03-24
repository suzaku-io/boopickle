#!/bin/bash

gitbook build doc _book
cd _book
git init
git commit --allow-empty -m 'update book'
git checkout -b gh-pages
touch .nojekyll
git add .
git commit -am "update book"
git push git@github.com:suzaku-io/boopickle gh-pages --force
cd ..
