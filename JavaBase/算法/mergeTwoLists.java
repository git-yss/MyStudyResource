/**
 * 合并两个有序链表
 * 链表 1：1 → 2 → 5 → 7
 * 链表 2：3 → 4 → 6 → 8
 * 思路：每次从两个链表头部，拿更小的那个放到结果里，直到全部拿完。
 */
class Solution {
    public ListNode mergeTwoLists(ListNode list1, ListNode list2) {
        // 虚拟头节点（用来接住新链表）,算法结束只需要dummy.next就可以去除掉-1
        ListNode dummy = new ListNode(-1);
        //显示最后的节点，每次循环添加值后都要往后挪动
        ListNode current = dummy;

        // 两个链表都没走完，就一直比大小
        while (list1 != null && list2 != null) {
            if (list1.val <= list2.val) {
                // 取小的接上
                current.next = list1;
                list1 = list1.next;
            } else {
                current.next = list2;
                list2 = list2.next;
            }
            // 指针往后走
            current = current.next;
        }

        // 把剩下没走完的链表直接接在尾部
        current.next = list1 != null ? list1 : list2;

        // 虚拟头的下一个就是真正的头
        return dummy.next;
    }
}

public class ListNode {
    int val;
    ListNode next;
    ListNode() {}
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}