// Student 1 - finds the largest number
public class Max {
    static int largest(int[] arr) {
        int best = arr[0];
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > best) { best = arr[i]; }
        }
        return best;
    }
}
