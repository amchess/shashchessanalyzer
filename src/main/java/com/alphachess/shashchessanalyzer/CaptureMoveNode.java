package com.alphachess.shashchessanalyzer;

import java.util.ArrayList;
import java.util.List;

import com.github.bhlangonijr.chesslib.move.Move;

public class CaptureMoveNode {
	private com.github.bhlangonijr.chesslib.move.Move move;
	private float materialDifference;
	private List<CaptureMoveNode> children;
	
	public CaptureMoveNode(Move move, float materialDifference) {
		super();
		this.move = move;
		this.materialDifference = materialDifference;
		this.children = new ArrayList<>();
	}
    public void addChild(CaptureMoveNode child) {
        children.add(child);
    }
	public com.github.bhlangonijr.chesslib.move.Move getMove() {
		return move;
	}
	public void setMove(com.github.bhlangonijr.chesslib.move.Move move) {
		this.move = move;
	}
	public float getMaterialDifference() {
		return materialDifference;
	}
	public void setMaterialDifference(float materialDifference) {
		this.materialDifference = materialDifference;
	}
	public List<CaptureMoveNode> getChildren() {
		return children;
	}
	public void setChildren(List<CaptureMoveNode> children) {
		this.children = children;
	}
}
