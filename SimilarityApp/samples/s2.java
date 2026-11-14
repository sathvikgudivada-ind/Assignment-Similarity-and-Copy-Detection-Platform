/* different names, spacing and comments - same logic */
public class Biggest{
 static int findMax(int[] numbers){
   int m=numbers[0];
   for(int k=1;k<numbers.length;k++){ if(numbers[k]>m){ m=numbers[k]; } }
   return m; }
}
