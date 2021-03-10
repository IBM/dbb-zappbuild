#! /bin/sh 
## Az/DBB Demo- Git clone v1.2 (njl) 
#export BASH_XTRACEFD=1
#exec 2>&1
#set -x
. $HOME/.profile 
MyRepo=$1 
MyWorkDir=$2 ; mkdir -p $MyWorkDir  ; cd $MyWorkDir 
branch=$3 
# Strip the prefix of the branch name for cloning 
branch=${branch##*"refs/heads/"}    

workSpace=$(basename $MyRepo) ;workSpace=${workSpace%.*}  
echo "**************************************************************" 
echo "**     Started:  Rocket-Git Clone on HOST/USER: $(uname -Ia)/$USER" 
echo "**                                 MyRepo:" $MyRepo 
echo "**                              MyWorkDir:" $PWD 
echo "**                              workSpace:" $workSpace 
echo "**                                 branch:" $3  "->" $branch  
git clone -b $branch $MyRepo  2>&1 
cd  $workSpace 

echo "Show status" 
git status  

echo "Show all Refs"  
git show-ref