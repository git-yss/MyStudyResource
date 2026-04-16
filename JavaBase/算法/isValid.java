/**
 * 有效的括号
 * 给定一个只包括 '('，')'，'{'，'}'，'['，']' 的字符串 s ，判断字符串是否有效。
 * 有效字符串需满足：
 * 左括号必须用相同类型的右括号闭合。
 * 左括号必须以正确的顺序闭合。
 * 每个右括号都有一个对应的相同类型的左括号。
 * 示例 1：
 * 输入：s = "()"
 * 输出：true
 *
 * 示例 2：
 * 输入：s = "()[]{}"
 * 输出：true
 * 思路：理由栈的前进后出的特性，用栈来存储左括号，当遇到右括号时，判断栈顶的左括号是否匹配，匹配则出栈，不匹配则返回false。
 */
class isValid {
    public boolean isValid(String s) {

        String[] split = s.split("");
        Stack<String> stack = new Stack<>();
        for (String ss : split) {
            if(!stack.isEmpty() && isMatch(stack.peek(), ss)){
                stack.pop();
            }else{
                stack.push(ss);
            }
        }
        if(stack.isEmpty()){
            return true;
        }else{
            return false;

        }
    }
    private static boolean isMatch(String left, String right) {
        if ("{".equals(left) && "}".equals(right)) return true;
        if ("[".equals(left) && "]".equals(right)) return true;
        if ("(".equals(left) && ")".equals(right)) return true;
        return false;
    }
}