package com.alphachess.shashchessanalyzer;

public class MoveRange {
	private String move;
	private int range;
	private int winProbability;
	
	public MoveRange(String move, int range, int winProbability) {
		super();
		this.move = move;
		this.range = range;
		this.winProbability=winProbability;
	}
	public String getMove() {
		return move;
	}
	public void setMove(String move) {
		this.move = move;
	}
	public int getRange() {
		return range;
	}
	public void setRange(int range) {
		this.range = range;
	}
	public int getWinProbability() {
		return winProbability;
	}
	public void setWinProbability(int winProbability) {
		this.winProbability = winProbability;
	}

}
