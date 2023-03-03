#!/bin/bash

echo "Cleaning files..."

rm -f *.svg
rm -f *.obj
rm -f *.exe

rm -f benchmark-*
rm -f output-*

rm -f data-random
rm -f results.csv

rm -rf asm
rm -rf bin