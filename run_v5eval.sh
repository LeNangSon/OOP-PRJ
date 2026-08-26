#!/usr/bin/env bash
# Chốt "MC best thật": khôi phục NGUYÊN bảng mc_wolf lịch sử ở commit 712b1ea (não v5, thô 1-bit,
# lean reward, pipeline cũ) rồi eval -DcoarseMC 50 seed x 3000 cạnh QL@fine (đã commit) + RBS.
# KHÔNG train lại -> tái lập chính xác não v5. Cuối cùng KHÔI PHỤC bảng canonical (mc_wolf fine3000).
set -uo pipefail
cd "$(dirname "$0")"

SNAP=/tmp/v5_classes
rm -rf "$SNAP" && cp -r target/classes "$SNAP"
CP="$SNAP"
for j in $(find "$HOME/.m2/repository/org/openjfx" -name '*.jar' 2>/dev/null | grep -vE 'sources|javadoc'); do CP="$CP:$j"; done
BC=org.openjfx.app.core.BrainCompare

echo "############ EVAL não v5 gốc (712b1ea) — $(date +%H:%M:%S) ############"
git show 712b1ea:qtables/mc_wolf.qtable > qtables/mc_wolf.qtable
echo ">> mc_wolf <- 712b1ea ($(wc -c <qtables/mc_wolf.qtable) bytes). EVAL -DcoarseMC 50x3000 $(date +%H:%M:%S)"
java -DcoarseMC=true -cp "$CP" $BC 50 3000 2>&1 | tee braincompare_v14.log
cp -f braincompare_perseed.csv braincompare_perseed_v14.csv
python3 stats_rliable.py braincompare_perseed.csv catches_per_1000 10000 2>&1 | grep -vE "Warning|warn\(" | tee stats_v14.txt
python3 stats_braincompare.py braincompare_perseed.csv catches_per_1000 20000 2>&1 | grep -vE "Warning|warn\(" | tee stats_v14_paired.txt

echo ">> KHÔI PHỤC mc_wolf canonical (fine3000) $(date +%H:%M:%S)"
git checkout -- qtables/mc_wolf.qtable
echo "############ V5 EVAL DONE — $(date +%H:%M:%S) ############"
sed -n '/Bat\/1000/p' braincompare_v14.log
