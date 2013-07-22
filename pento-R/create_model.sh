#!/bin/bash
rundir=`pwd`
clojure_dir=../pento-clojure
indir="$rundir/input-data/"
outdir="$rundir/output-data/"
cd $clojure_dir
lein run -m zolo.pento.feature-extraction $indir/good_emails.prn $outdir/train_1.csv 1
lein run -m zolo.pento.feature-extraction $indir/bad_emails.prn $outdir/train_2.csv 0
lein run -m zolo.pento.feature-extraction $indir/good_email.json $outdir/test_1.csv 1
lein run -m zolo.pento.feature-extraction $indir/bad_email.json $outdir/test_2.csv 0
lein run -m zolo.pento.feature-extraction header $outdir/header.csv 0 
cd $outdir
for file in header.csv train_1.csv train_2.csv
do
  cat $file
  echo
done | grep -v "^\s*$" >  train.csv
for file in header.csv test_1.csv test_2.csv
do
  cat $file
  echo
done | grep -v "^\s*$" >  test.csv

cd $rundir
Rscript create_model.R | tr -d '"' | sed -e "s/^\[1\]//"
cat coeffs.csv | sed -e "s/\([a-z]\)\.\([a-z]\)/\1-\2/g" | tr -d '"' | sed -e "s/^/:/" | grep [0-9] | tr "\n" " "
