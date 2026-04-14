/**
 * 删除倒数第 N 个节点
 *
 * 输入：head = [1,2,3,4,5], n = 2
 * 输出：[1,2,3,5]
 * 思路：循环遍历链表，获取链表长度，根据N算出循环第几个节点就删除，并将上一个节点的next指向当前节点的next，这样删除了当前节点
 */
class Solution {
    public ListNode removeNthFromEnd(ListNode head, int n) {
        int len =0;
        ListNode dum = head;
        while(dum != null){
            len++;
            dum = dum.next;
        }
        ListNode dum1 = head;
        int index = len - n;
        int i =0;
        if(index == 0){
            return head.next;
        }
        while(dum1 != null){
            if(i ==index-1){
                dum1.next = dum1.next.next;
                break;
            }
            dum1 = dum1.next;
            i++;
        }
        return head;
    }

}

public class ListNode {
    int val;
    ListNode next;
    ListNode() {}
    ListNode(int val) { this.val = val; }
    ListNode(int val, ListNode next) { this.val = val; this.next = next; }
}