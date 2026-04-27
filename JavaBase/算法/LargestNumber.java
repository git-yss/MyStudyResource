class LargestNumber {
    /**
     * 小组中每位成员都有一张卡片，卡片上是 6 位内的正整数。将所有成员的卡片数字连起来可以组成多种不同的数字，要求计算能组成的最大数字。
     * 输入4589,101,41425,9999
     * 输出9999458941425101
     * @param args
     */
    public static void main(String[] args) {
        String str="4589,101,41425,9999";
        String[] strings = str.split(",");
        Arrays.sort(strings, (a, b) -> (b + a).compareTo(a + b));
        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            sb.append(s);
        }
        System.out.println(sb.toString());
    }


}