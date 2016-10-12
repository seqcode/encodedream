use strict;

my $facname = $ARGV[0];
my $cellname = $ARGV[1];

my $topXDnasePeaks = $ARGV[2]; ## no of top ranked dnase domains to take
my $c1score_cutoff = $ARGV[3];
my $b = $ARGV[4]; ## steps around narrow peak to look for seed (b=3 corresponds to 500bp around)

## First load scores
my $c1scoreFname = "/storage/home/auk262/group/projects/general/encode-dream/classifier_c1_scores/".$facname."_C1_scores.txt";
open(C1,$c1scoreFname);
my @scores;
while(<C1>){
	chomp $_;
	push(@scores,$_);
}
close(C1);

## Now roll down  enriched dnase domain indices

my $domainFname = "/storage/home/auk262/group/projects/general/encode-dream/C3_cellspecific_classifier/".$cellname."_distal_dnase_domains.tab";
print $domainFname,"\n";
open(D1,$domainFname);

my $tp =0;
my $fp =0;

my $noAdded = 0;

while(<D1>){# and $noAdded < $topXDnasePeaks ){
	chomp $_;
	my @pieces = split("\t",$_);
	my $max_score = 0;
	my $max_ind = 0;
	my $foundNarrow = 0;
	for(my $x=-1*$b;$x<=$b; $x++){
		if($scores[$pieces[4]+$x] gt $max_score){
			$max_score = $scores[$pieces[4]+$x];
			$max_ind = $pieces[4]+$x;
		}
	}
	if($max_score gt $c1score_cutoff){
		print $pieces[0],"\t",$pieces[4],"\n"; 
	}

	$noAdded++;
	if($noAdded > $topXDnasePeaks){
		last;
	}
}
close(D1);



