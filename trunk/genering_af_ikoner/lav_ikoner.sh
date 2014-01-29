
# Se http://developer.android.com/design/style/iconography.html

odt2txt ikoner_stamdokument.odt | while read l; 
do 
	i=`echo $l|cut -f1 -d' '`; 
	n=`echo $l|cut -f2 -d' '`; 
	if [ "$n" = "x" -o "x$n" = "x" ]; then continue; fi; 
	echo "$i $n"; 
	
	for dpi in mdpi hdpi xhdpi; 
	do
		dp=32
		faktor=1
		if [ "$dpi" = "hdpi" ]; then faktor=1.5; fi; 
		if [ "$dpi" = "xhdpi" ]; then faktor=2; fi; 
		if [ "$dpi" = "xxhdpi" ]; then faktor=3; fi; 
		xp=`echo "$dp * $faktor"|bc`

		sti=../DRRadiov3/res/drawable-${dpi}/
#	konv="convert -background none -font dr-icons-Regular -pointsize $p  -gravity center"
		konv="convert -background none -font dr-icons-Regular -size ${xp}x${xp}  -gravity center"

		fn="$sti/${n}_blaa.png"; $konv -fill '#55b9c4' label:"$i" $fn; 
		fn="$sti/${n}_graa40.png"; $konv -fill '#999999' label:"$i" $fn; 
		fn="$sti/${n}_hvid.png";   $konv -fill white label:"$i" $fn; 


#convert -scale 36x36 ic_padlock.png ../../res/drawable-ldpi/ic_launcher_padlock.png
#convert -scale 48x48 ic_padlock.png ../../res/drawable-mdpi/ic_launcher_padlock.png
#convert -scale 72x72 ic_padlock.png ../../res/drawable-hdpi/ic_launcher_padlock.png

	done

done



