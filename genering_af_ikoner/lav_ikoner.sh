
# Se http://developer.android.com/design/style/iconography.html

odt2txt ikoner_stamdokument.odt | while read l; 
do 
	i=`echo $l|cut -f1 -d' '`; 
	n=`echo $l|cut -f2 -d' '|tr '[:upper:]' '[:lower:]'`; 
	if [ "$n" = "x" -o "x$n" = "x" ]; then continue; fi; 
	echo -n "'$i' '$n'  ->  "; 
	if [[ $n != dri_* ]]; then n=dri_$n; fi; 
	
#	for dpi in mdpi hdpi xhdpi xxhdpi; 
	for dpi in xhdpi; 
	do
		dp=32; #  men resten er 'polstring' til ikonet
		dp_indh=24; # 32, men resten er 'polstring' til ikonet
		faktor=1
		if [ "$dpi" = "hdpi" ]; then faktor=1.5; fi; 
		if [ "$dpi" = "xhdpi" ]; then faktor=2; fi; 
		if [ "$dpi" = "xxhdpi" ]; then faktor=3; fi; 
		xp=`echo "$dp * $faktor"|bc`
		xpi=`echo "$dp_indh * $faktor"|bc`
#		xp=512
#		dpi=ios

		sti=../DRRadiov3/resny/drawable-${dpi}
#		sti=res/drawable-${dpi}
		mkdir -p $sti
#		konv="convert -background none -font dr-icons-Regular -size ${xpi}x${xpi} -gravity center -trim -extent ${xp}x${xp}"
		konv="convert -background none -font icomoon -size ${xpi}x${xpi} -gravity center -trim -extent ${xp}x${xp}"

#		fn="$sti/${n}.png"; $konv -fill black label:"$i" $fn; 
		echo $konv -fill '#55b9c4' label:"$i" $fn; 
		fn="$sti/${n}_blaa.png"; $konv -fill '#55b9c4' label:"\\$i" $fn; 
		fn="$sti/${n}_graa40.png"; $konv -fill '#999999' label:"\\$i" $fn; 
		fn="$sti/${n}_hvid.png";   $konv -fill white label:"\\$i" $fn; 
		echo -n "  $fn"

	done
	echo 

done



