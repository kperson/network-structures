#!/bin/bash
if ! [ -f .sbtopts ];then
  cp .sbtopts.sample .sbtopts
  cp repositories.prod repositories
fi
