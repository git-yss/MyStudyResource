/**
 * 链表反转
 * 输入：head = [1,2,3,4,5]
 * 输出：[5,4,3,2,1]
 * 思路1->2->3->4->5   指针反转就ok
 */
class Solution {
    public ListNode reverseList(ListNode head) {
        //创建空节点（链表算法常用技巧，这样遍历表链时就可以从第一个node进行操作而不是从第二个才开始操作），pre也是我们用来储存反转好的链表，是最后方法结束后返回的变量
        ListNode pre = null;
        //暂存当前最新的链表 后续会减少链表数据，为了仿造遍历这样的效果
        ListNode cur = head;
        while (cur != null) {
            //存储下一个链表作为cur，让他继续遍历
            ListNode temp = cur.next;
            //将当前node的下一节点指向我们前面存储的pre（反转后节点储存点）里
            cur.next = pre;
            //原来的cur其实就断了，比如1->null后  2->3->4_5就被丢了
            pre = cur;
            //将第一步暂存的2->3->4_5接着循环
            cur = temp;
        }
        return pre;
    }
}




