#!/usr/bin/env bash

git config --global user.email "travis@travis-ci.org"
git config --global user.name "Travis CI"
git remote rm origin
git remote add origin https://${GH_TOKEN}@github.com/district0x/lein-solc.git > /dev/null 2>&1

lein release $1

exit $?
