package com.alphachess.shashchessanalyzer;

import ictk.boardgame.chess.ChessMove;
import net.andreinc.neatchess.client.model.Move;

public class MoveInfo {
	public static final String FULL_STRENGTH_ANALYSIS = "Full strength analysis";
	public static final String HANDICAPPED_ANALYSIS = "Handicapped analysis";
	public static final int CAPABLANCA_THRESHOLD = 9;
	public static final int LOW_THRESHOLD = 20;
	public static final int LOW_MIDDLE_THRESHOLD = 41;
	public static final int MIDDLE_THRESHOLD = 59;
	public static final int HIGH_MIDDLE_THRESHOLD = 93;
	public static final int HIGH_THRESHOLD = 160;
	public static final String HIGH_PETROSIAN = "High Petrosian";
	public static final String HIGH_MIDDLE_PETROSIAN = "Middle High Petrosian";
	public static final String MIDDLE_PETROSIAN = "Middle Petrosian";
	public static final String MIDDLE_LOW_PETROSIAN = "Middle Low Petrosian";
	public static final String LOW_PETROSIAN = "Low Petrosian";
	public static final String CAOS_PETROSIAN_CAPABLANCA = "Caos Petrosian-Capablanca";
	public static final String CAPABLANCA = "Capablanca";
	public static final String CAOS_TAL_CAPABLANCA = "Caos Capablanca-Tal";
	public static final String LOW_TAL = "Low Tal";
	public static final String LOW_MIDDLE_TAL = "Low Middle Tal";
	public static final String MIDDLE_TAL = "Middle Tal";
	public static final String MIDDLE_HIGH_TAL = "Middle High Tal";
	public static final String HIGH_TAL = "High Tal";
	public static final String CAOS_TAL_CAPABLANCA_PETROSIAN = "Caos Tal-Capablanca-Petrosian";	
	private Move move;
	private ChessMove chessMove;
	private int score;
	private int depth;
	private String positionType;
	
	
	public MoveInfo(Move move, ChessMove chessMove,int ply) {
		super();
		this.move = move;
		this.chessMove = chessMove;
		this.score = ((Double) (move.getStrength().getScore() * 100)).intValue();
		this.depth = move.getDepth();
		this.positionType = getPositionType(score,ply);
	}
	public Move getMove() {
		return move;
	}
	public ChessMove getChessMove() {
		return chessMove;
	}
	public int getScore() {
		return score;
	}
	public int getDepth() {
		return depth;
	}
	public String getPositionType() {
		return positionType;
	}
	public void setMove(Move move) {
		this.move = move;
	}
	public void setChessMove(ChessMove chessMove) {
		this.chessMove = chessMove;
	}
	public void setScore(int score) {
		this.score = score;
	}
	public void setDepth(int depth) {
		this.depth = depth;
	}
	public void setPositionType(String positionType) {
		this.positionType = positionType;
	}
	private String getPositionType(int score, int ply) {
		WinProbabilityByShashin winProbabilityByShashin=new WinProbabilityByShashin();
		int winProbability=winProbabilityByShashin.getWinProbability(score, ply);
		int rangeValue=winProbabilityByShashin.getRange(winProbability);
		return winProbabilityByShashin.getRangeDescription(rangeValue);
	}
}
