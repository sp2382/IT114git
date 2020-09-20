public class JavaLoops {

	public static void main(String[] args) {

		int[] nums = { 1, 3, 5, 6, 7, 9, 10 };

		for (int i = 0; i < nums.length; i++) {
			if (nums[i] % 2 == 0) {
				System.out.println(nums[i]);
			}
		}

	}

}