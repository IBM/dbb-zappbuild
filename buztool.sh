#! /bin/sh
echo "Hi, I'm a buztool dummy"
BuzToolOutFID=$7   # S/B buztool.output location
echo "Buztool output file ${BuzToolOutFID}"
echo "Buztool Dummy Output Recode 001" > ${BuzToolOutFID}
echo "Buztool Dummy Output Recode 002" >> ${BuzToolOutFID}
echo "Buztool Dummy Output Recode 003" >> ${BuzToolOutFID}
exit 0