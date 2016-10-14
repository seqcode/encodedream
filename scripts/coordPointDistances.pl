#!/usr/bin/perl

#Print distance from each entry in file1 to the closest entry in file2
my $file1 = $ARGV[0];
my $file2 = $ARGV[1];
unless(open(F1, $file1)){
	die "Cannot open peaks file\n";}
unless(open(F2, $file2)){
	die "Cannot open annot file\n";}

%chrID =();
@bList =();
$numB=0; $numChr=0;
@chrCount=();
while($currLine = <F2>){
    @curr = split(/\s+/, $currLine);
    if($curr[0] =~ m/\:/){
	@curr2 = split(/\:/, $curr[0]);
	$chr = $curr2[0]; $chr =~ s/chr//g;
	@curr3 = split(/\-/, $curr2[1]);
	$start = $curr3[0];
	if($#curr3>0){
	    $stop = $curr3[1];
	}else{
	    $stop=$start+1;
	}
	if($#curr>=2){
	    @curr4 = split(/\:/, $curr[2]);
	    $peak = $curr4[1];
	}else{
	    $peak = ($stop+$start)/2;
	}
    }else{
	$chr = $curr[0]; $chr =~ s/chr//g;
	$start = $curr[1];
	$stop = $curr[2];
    }
    if(!defined($chrID{$chr})){$chrID{$chr}=$numChr; $chrCount[$numChr]=0;$numChr++;}
    $thisChr = $chrID{$chr};
    $numC=$chrCount[$thisChr];
    $bList[$thisChr][$numC][0]=$chr;
    $bList[$thisChr][$numC][1]=$start;
    $bList[$thisChr][$numC][2]=$stop;
    $chrCount[$thisChr]++;
}

#Sort the bList structure
foreach my $chr (keys %chrID) {
    if(defined($chrID{$chr})){
	$thisChr=$chrID{$chr};
	$ccount=$chrCount[$thisChr];

	@bListC = ();
	#copy
	for($j=0; $j<$ccount; $j++){
            $bListC[$j][0] = $bList[$thisChr][$j][0];
            $bListC[$j][1] = $bList[$thisChr][$j][1];
            $bListC[$j][2] = $bList[$thisChr][$j][2];
	}
	#Sort 
	@sorted = sort { $a->[1] <=> $b->[1] } @bListC;
	#copy
	for($j=0; $j<$ccount; $j++){
            $bList[$thisChr][$j][0] = $sorted[$j][0];
            $bList[$thisChr][$j][1] = $sorted[$j][1];
            $bList[$thisChr][$j][2] = $sorted[$j][2];
	}
    }
}


#Compare
while($currLine = <F1>){
    chomp($currLine);
    @curr = split(/\s+/, $currLine);
    if($curr[0] =~ m/\:/){
	@curr2 = split(/\:/, $curr[0]);
	$chr = $curr2[0]; $chr =~ s/chr//g;
	@curr3 = split(/\-/, $curr2[1]);
	$start = $curr3[0];
	if($#curr3>0){
	    $stop = $curr3[1];
	}else{
	    $stop=$start+1;
	}
	    if($#curr>=2){
		@curr4 = split(/\:/, $curr[2]);
		$peak = $curr4[1];
	    }else{
		$peak = int(($stop+$start)/2);
	    }
    }else{
	$chr = $curr[0]; $chr =~ s/chr//g;
	$start = $curr[1];
	$stop = $curr[2];
	$peak = int(($stop+$start)/2);
    }

    
    $minDist = 100000000;

    #binary search bList starts (=TSSs)
    if(defined($chrID{$chr})){
	$thisChr=$chrID{$chr};
	$ccount=$chrCount[$thisChr];
	
	$lower = 0;
	$upper = $ccount-1;
	$mid = ($upper+$lower)/2;

	while($upper-$lower > 10){
	    if($peak <= $bList[$thisChr][$mid][1]){
		$upper = $mid
	    }else{
		$lower = $mid;
	    }
	    $mid = ($upper+$lower)/2;
	}

	#Iterate through B list
	for($j=$lower; $j<=$upper; $j++){
	    $dist = abs($peak-$bList[$thisChr][$j][1]);
	    if($dist<$minDist){
		$minDist=$dist;
	    }		
	}
    }
    print "$minDist\n";
}
close(F1);
close(F2);

