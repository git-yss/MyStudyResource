/**
 * 判断链表是否有环
 * 思路：快慢指针
 * 1->3->2->4-3
 */
public class Solution {
    public boolean hasCycle(ListNode head) {
        //起点相同，设置两个步调不同的指针，快指针每次走两步，慢指针每次走一步，如果有环那么快慢指针一定会回来并且与快指针会重新追上慢指针相遇，如果没环，快指针一定会走完
        ListNode slow = head;
        ListNode fast = head;
        while (fast != null && fast.next != null) {
            slow = slow.next;
            fast = fast.next.next;
            if (slow == fast) {
                return true;
            }
        }
        return false;
    }
}
public class ListNode {
    int val;
    ListNode next;
    ListNode() {}
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}