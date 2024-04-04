package com.alphachess.shashchessanalyzer;
import java.util.ArrayList;
import java.util.List;

class TreeNode {
    int value;
    List<TreeNode> children;

    public TreeNode(int value) {
        this.value = value;
        this.children = new ArrayList<>();
    }

    public void addChild(TreeNode child) {
        children.add(child);
    }
}

public class MinimaxAlgorithm {

    public static int minimax(TreeNode node, boolean maximizingPlayer) {
        if (node.children.isEmpty()) {
            // If it's a leaf node, return its value
            return node.value;
        }

        if (maximizingPlayer) {
            int maxEval = Integer.MIN_VALUE;
            for (TreeNode child : node.children) {
                int eval = minimax(child, false);
                maxEval = Math.max(maxEval, eval);
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (TreeNode child : node.children) {
                int eval = minimax(child, true);
                minEval = Math.min(minEval, eval);
            }
            return minEval;
        }
    }

    public static void main(String[] args) {
        // Constructing a simple tree for demonstration
        TreeNode root = new TreeNode(0);
        TreeNode child1 = new TreeNode(-1);
        TreeNode child2 = new TreeNode(-1);
        TreeNode grandChild1 = new TreeNode(-2);
 
        root.addChild(child1);
        root.addChild(child2);
        child2.addChild(grandChild1);

        // Running minimax algorithm on the tree
        int result = minimax(root, true); // Assuming root is the maximizing player
        System.out.println("The optimal value is: " + result);
    }
}
