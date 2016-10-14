#!/usr/bin/perl

my $myFile = $ARGV[0];
my $useGene = 1;
if($#ARGV>0){
    $useGene = $ARGV[1];
}
unless(open(FILE, $myFile)){
	die "Cannot open file\n";}
@lines = <FILE>;
$sum=0;
for($x=0; $x<=$#lines; $x++){
    @currLine = split(/\s+/, $lines[$x]);

    if(($useGene==1 && $currLine[2] eq "gene") || ($useGene==0 && ($currLine[2] eq "mRNA" || $currLine[2] eq "transcript"))){
	$chr = $currLine[0]; $chr=~s/chr//g;
	$start = $currLine[3];
	$end = $currLine[4];
	$strand = $currLine[6];
	$name = $currLine[8];
	if($strand eq "+"){
	    print "$chr:$start:$strand\n";
	}else{
	    print "$chr:$end:$strand\n";
	}
    }
}
close(FILE);
