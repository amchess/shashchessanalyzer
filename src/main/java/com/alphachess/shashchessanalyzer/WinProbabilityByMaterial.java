package com.alphachess.shashchessanalyzer;

import java.io.IOException;
import java.util.logging.Logger;

import ictk.boardgame.chess.ChessBoard;
import ictk.boardgame.chess.ChessPiece;
import ictk.boardgame.chess.Square;
import ictk.boardgame.chess.io.FEN;

public class WinProbabilityByMaterial {
	public static final int MAX_DEPTH = 240;
	public static final int NORMALIZE_TO_PAWN_VALUE = 356;
	static final double[] as = { -37.45051876, 121.19101539, -132.78783573, 420.70576692 };
	static final double[] bs = { 90.26261072, -137.26549898, 71.10130540, 51.35259597 };
	private static final Logger logger = Logger.getLogger(WinProbabilityByMaterial.class.getName());

	public enum RangeDescription 
	{

		HIGH_PETROSIAN("High Petrosian"),
		HIGH_MIDDLE_PETROSIAN("Middle High Petrosian"),
		MIDDLE_PETROSIAN("Middle Petrosian"),
		MIDDLE_LOW_PETROSIAN("Middle Low Petrosian"),
		LOW_PETROSIAN("Low Petrosian"),
		CAOS_PETROSIAN_CAPABLANCA("Caos Petrosian-Capablanca"),
		CAPABLANCA("Capablanca"),
		CAOS_TAL_CAPABLANCA("Caos Capablanca-Tal"),
		LOW_TAL("Low Tal"),
		LOW_MIDDLE_TAL("Low Middle Tal"),
		MIDDLE_TAL("Middle Tal"),
		MIDDLE_HIGH_TAL("Middle High Tal"),
		HIGH_TAL("High Tal"),
		CAOS_TAL_CAPABLANCA_PETROSIAN("Caos Tal-Capablanca-Petrosian");
		
	    private String description;
	 
	    RangeDescription(String description) {
	        this.description = description;
	    }
	 
	    public String getDescription() {
	        return description;
	    }
	}
	public static String getAbbreviateRangeDescription(String rangeDescription) {
	    switch (rangeDescription) {
	        case MoveInfo.CAOS_TAL_CAPABLANCA_PETROSIAN:
	        	return "CTCP";
	        case MoveInfo.HIGH_PETROSIAN:
	        	return "HP";
	        case MoveInfo.HIGH_MIDDLE_PETROSIAN:
	        	return "MHP";
	        case MoveInfo.MIDDLE_PETROSIAN:
	        	return "MP";	
	        case MoveInfo.MIDDLE_LOW_PETROSIAN:
	        	return "MLP";
	        case MoveInfo.LOW_PETROSIAN:
	        	return "LP";
	        case MoveInfo.CAOS_PETROSIAN_CAPABLANCA:
	        	return "CCP";
	        case MoveInfo.CAPABLANCA:
	        	return "C";
	        case MoveInfo.CAOS_TAL_CAPABLANCA:
	        	return "CCT";
	        case MoveInfo.LOW_TAL:
	        	return "LT";
	        case MoveInfo.LOW_MIDDLE_TAL:
	        	return "LMT";
	        case MoveInfo.MIDDLE_TAL:
	        	return "MT";
	        case MoveInfo.MIDDLE_HIGH_TAL:
	        	return "MHT";
	        case MoveInfo.HIGH_TAL:
	        	return "HT";
	        default: return null;
	    }
	}	
	
	public enum Range {
		SHASHIN_POSITION_TAL_CAPABLANCA_PETROSIAN(7), SHASHIN_POSITION_HIGH_PETROSIAN(-6),
		SHASHIN_POSITION_MIDDLE_HIGH_PETROSIAN(-5), SHASHIN_POSITION_MIDDLE_PETROSIAN(-4),
		SHASHIN_POSITION_MIDDLE_LOW_PETROSIAN(-3), SHASHIN_POSITION_LOW_PETROSIAN(-2),
		SHASHIN_POSITION_CAPABLANCA_PETROSIAN(-1), SHASHIN_POSITION_CAPABLANCA(0), SHASHIN_POSITION_CAPABLANCA_TAL(1),
		SHASHIN_POSITION_LOW_TAL(2), SHASHIN_POSITION_MIDDLE_LOW_TAL(3), SHASHIN_POSITION_MIDDLE_TAL(4),
		SHASHIN_POSITION_MIDDLE_HIGH_TAL(5), SHASHIN_POSITION_HIGH_TAL(6);

		private int value;

		private Range(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public static void main(String[] args) {
		String fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
		int totalMaterial = getTotalMaterial(fen);
		String msgMaterial = "Total material:" + totalMaterial;
		logger.info(msgMaterial);
		String msgWinProbability = "Win probability:" + getWinProbability(25, totalMaterial);
		logger.info(msgWinProbability);
	}

	// Function to calculate win rate parameters
	static WinRateParams winRateParams(int materialClamp) {
		double m = materialClamp / 58.0;
		double a = (((as[0] * m + as[1]) * m + as[2]) * m) + as[3];
		double b = (((bs[0] * m + bs[1]) * m + bs[2]) * m) + bs[3];
		return new WinRateParams(a, b);
	}

	public enum Threeshold {
		SHASHIN_LOW_TAL_THRESHOLD(76), SHASHIN_MIDDLE_LOW_TAL_THRESHOLD(81), SHASHIN_MIDDLE_TAL_THRESHOLD(88),
		SHASHIN_MIDDLE_HIGH_TAL_THRESHOLD(91), SHASHIN_HIGH_TAL_THRESHOLD(96), SHASHIN_CAPABLANCA_THRESHOLD(51),
		SHASHIN_LOW_PETROSIAN_THRESHOLD(24), SHASHIN_MIDDLE_LOW_PETROSIAN_THRESHOLD(19),
		SHASHIN_MIDDLE_PETROSIAN_THRESHOLD(12), SHASHIN_MIDDLE_HIGH_PETROSIAN_THRESHOLD(9),
		SHASHIN_HIGH_PETROSIAN_THRESHOLD(4);

		private int value;

		private Threeshold(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	// Function to get win probability
	public static byte getWinProbability(int score, int materialClamp) {
		WinRateParams params = winRateParams(materialClamp);
		double a = params.a;
		double b = params.b;
		long valueClamp = (long) (score * a / 100);
		double wdlW = 0.5 + 1000 / (1 + Math.exp((a - valueClamp) / b));
		double wdlL = 0.5 + 1000 / (1 + Math.exp((a - (-valueClamp)) / b));
		double wdlD = 1000 - wdlW - wdlL;
		return (byte) Math.round((wdlW + wdlD / 2.0) / 10.0);
	}

	public static byte getWinProbabilityFromValue(int valueClamp, int materialClamp) {
		WinRateParams params = winRateParams(materialClamp);
		double a = params.a;
		double b = params.b;
		double wdlW = 0.5 + 1000 / (1 + Math.exp((a - valueClamp) / b));
		double wdlL = 0.5 + 1000 / (1 + Math.exp((a - (-valueClamp)) / b));
		double wdlD = 1000 - wdlW - wdlL;
		return (byte) Math.round((wdlW + wdlD / 2.0) / 10.0);
	}

	public static int getWinProbabilityByScore(int score, String fen) {
		int materialClamp = Math.max(17, Math.min(getTotalMaterial(fen), 78));
		return getWinProbability(score, materialClamp);
	}

	public static int getTotalMaterial(String fen) {
		int totalMaterial = 0;
		try {
			ChessBoard chessBoard = (ChessBoard) (new FEN().stringToBoard(fen));
			for (int rank = 1; rank <= 8; rank++)
				for (int file = 1; file <= 8; file++) {
					Square currentSquare = chessBoard.getSquare(file, rank);
					ChessPiece currentChessPiece = (ChessPiece) (currentSquare.getPiece());
					if (currentChessPiece != null) {
						int currentChessPieceIndex = currentChessPiece.getIndex();
						switch (currentChessPieceIndex) {
						case 5:
						case 75: {
							totalMaterial += 1;
							break;
						}
						case 3:
						case 4:
						case 73:
						case 74: {
							totalMaterial += 3;
							break;
						}
						case 72:
						case 2: {
							totalMaterial += 5;
							break;
						}
						case 71:
						case 1: {
							totalMaterial += 9;
							break;
						}
						default: {
							break;
						}

						}
					}
				}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return totalMaterial;
	}

	public int getComplexityGap(int currentPlayedMoveWinProbability, int previousPlayedMoveWinProbability) {
		int currentPlayedMoveWinProbabilityRange = getRange(currentPlayedMoveWinProbability);
		int currentPreviousMoveWinProbabilityRange = getRange(previousPlayedMoveWinProbability);
		return Math.abs(currentPlayedMoveWinProbabilityRange - currentPreviousMoveWinProbabilityRange);
	}

	public static String getRangeDescription(int rangeValue) {
		Range[] values = Range.values();
		Range range = null;
		for (Range currentRange : values) {
			if (currentRange.getValue() == rangeValue) {
				range = currentRange;
				break;
			}
		}
		if (range != null) {
			switch (range) {
			case SHASHIN_POSITION_TAL_CAPABLANCA_PETROSIAN:
				return RangeDescription.CAOS_TAL_CAPABLANCA_PETROSIAN.getDescription();
			case SHASHIN_POSITION_HIGH_PETROSIAN:
				return RangeDescription.HIGH_PETROSIAN.getDescription();
			case SHASHIN_POSITION_MIDDLE_HIGH_PETROSIAN:
				return RangeDescription.HIGH_MIDDLE_PETROSIAN.getDescription();
			case SHASHIN_POSITION_MIDDLE_PETROSIAN:
				return RangeDescription.MIDDLE_PETROSIAN.getDescription();
			case SHASHIN_POSITION_MIDDLE_LOW_PETROSIAN:
				return RangeDescription.MIDDLE_LOW_PETROSIAN.getDescription();
			case SHASHIN_POSITION_LOW_PETROSIAN:
				return RangeDescription.LOW_PETROSIAN.getDescription();
			case SHASHIN_POSITION_CAPABLANCA_PETROSIAN:
				return RangeDescription.CAOS_PETROSIAN_CAPABLANCA.getDescription();
			case SHASHIN_POSITION_CAPABLANCA:
				return RangeDescription.CAPABLANCA.getDescription();
			case SHASHIN_POSITION_CAPABLANCA_TAL:
				return RangeDescription.CAOS_TAL_CAPABLANCA.getDescription();
			case SHASHIN_POSITION_LOW_TAL:
				return RangeDescription.LOW_TAL.getDescription();
			case SHASHIN_POSITION_MIDDLE_LOW_TAL:
				return RangeDescription.LOW_MIDDLE_TAL.getDescription();
			case SHASHIN_POSITION_MIDDLE_TAL:
				return RangeDescription.MIDDLE_TAL.getDescription();
			case SHASHIN_POSITION_MIDDLE_HIGH_TAL:
				return RangeDescription.MIDDLE_HIGH_TAL.getDescription();
			case SHASHIN_POSITION_HIGH_TAL:
				return RangeDescription.HIGH_TAL.getDescription();
			default:
				return null;
			}
		}
		return null;
	}

	public static int getRange(int winProbability) {
		if (isHighPetrosian(winProbability)) {
			return Range.SHASHIN_POSITION_HIGH_PETROSIAN.getValue();
		} else if (isMiddleHighPetrosian(winProbability)) {
			return Range.SHASHIN_POSITION_MIDDLE_HIGH_PETROSIAN.getValue();
		} else if (isMiddlePetrosian(winProbability)) {
			return Range.SHASHIN_POSITION_MIDDLE_PETROSIAN.getValue();
		} else if (isMiddleLowPetrosian(winProbability)) {
			return Range.SHASHIN_POSITION_MIDDLE_LOW_PETROSIAN.getValue();
		} else if (isLowPetrosian(winProbability)) {
			return Range.SHASHIN_POSITION_LOW_PETROSIAN.getValue();
		} else if (isCapablancaPetrosian(winProbability)) {
			return Range.SHASHIN_POSITION_CAPABLANCA_PETROSIAN.getValue();
		} else if (isCapablanca(winProbability)) {
			return Range.SHASHIN_POSITION_CAPABLANCA.getValue();
		} else if (isCapablancaTal(winProbability)) {
			return Range.SHASHIN_POSITION_CAPABLANCA_TAL.getValue();
		} else if (isLowTal(winProbability)) {
			return Range.SHASHIN_POSITION_LOW_TAL.getValue();
		} else if (isMiddleLowTal(winProbability)) {
			return Range.SHASHIN_POSITION_MIDDLE_LOW_TAL.getValue();
		} else if (isMiddleTal(winProbability)) {
			return Range.SHASHIN_POSITION_MIDDLE_TAL.getValue();
		} else if (isMiddleHighTal(winProbability)) {
			return Range.SHASHIN_POSITION_MIDDLE_HIGH_TAL.getValue();
		} else if (isHighTal(winProbability)) {
			return Range.SHASHIN_POSITION_HIGH_TAL.getValue();
		}
		return Range.SHASHIN_POSITION_TAL_CAPABLANCA_PETROSIAN.getValue();
	}

	// Metodi ausiliari per verificare le soglie
	private static boolean isHighPetrosian(int winProbability) {
		return winProbability <= Threeshold.SHASHIN_HIGH_PETROSIAN_THRESHOLD.getValue();
	}

	private static boolean isMiddleHighPetrosian(int winProbability) {
		return winProbability > Threeshold.SHASHIN_HIGH_PETROSIAN_THRESHOLD.getValue()
				&& winProbability <= Threeshold.SHASHIN_MIDDLE_HIGH_PETROSIAN_THRESHOLD.getValue();
	}

	private static boolean isMiddlePetrosian(int winProbability) {
		return winProbability > Threeshold.SHASHIN_MIDDLE_HIGH_PETROSIAN_THRESHOLD.getValue()
				&& winProbability <= Threeshold.SHASHIN_MIDDLE_PETROSIAN_THRESHOLD.getValue();
	}

	private static boolean isMiddleLowPetrosian(int winProbability) {
		return winProbability > Threeshold.SHASHIN_MIDDLE_PETROSIAN_THRESHOLD.getValue()
				&& winProbability <= Threeshold.SHASHIN_MIDDLE_LOW_PETROSIAN_THRESHOLD.getValue();
	}

	private static boolean isLowPetrosian(int winProbability) {
		return winProbability > Threeshold.SHASHIN_MIDDLE_LOW_PETROSIAN_THRESHOLD.getValue()
				&& winProbability <= Threeshold.SHASHIN_LOW_PETROSIAN_THRESHOLD.getValue();
	}

	private static boolean isCapablancaPetrosian(int winProbability) {
		return winProbability > Threeshold.SHASHIN_LOW_PETROSIAN_THRESHOLD.getValue()
				&& winProbability <= 100 - Threeshold.SHASHIN_CAPABLANCA_THRESHOLD.getValue();
	}

	private static boolean isCapablanca(int winProbability) {
		return winProbability > (100 - Threeshold.SHASHIN_CAPABLANCA_THRESHOLD.getValue())
				&& winProbability < Threeshold.SHASHIN_CAPABLANCA_THRESHOLD.getValue();
	}

	private static boolean isCapablancaTal(int winProbability) {
		return winProbability < Threeshold.SHASHIN_LOW_TAL_THRESHOLD.getValue()
				&& winProbability >= Threeshold.SHASHIN_CAPABLANCA_THRESHOLD.getValue();
	}

	private static boolean isLowTal(int winProbability) {
		return winProbability < Threeshold.SHASHIN_MIDDLE_LOW_TAL_THRESHOLD.getValue()
				&& winProbability >= Threeshold.SHASHIN_LOW_TAL_THRESHOLD.getValue();
	}

	private static boolean isMiddleLowTal(int winProbability) {
		return winProbability < Threeshold.SHASHIN_MIDDLE_TAL_THRESHOLD.getValue()
				&& winProbability >= Threeshold.SHASHIN_MIDDLE_LOW_TAL_THRESHOLD.getValue();
	}

	private static boolean isMiddleTal(int winProbability) {
		return winProbability < Threeshold.SHASHIN_MIDDLE_HIGH_TAL_THRESHOLD.getValue()
				&& winProbability >= Threeshold.SHASHIN_MIDDLE_TAL_THRESHOLD.getValue();
	}

	private static boolean isMiddleHighTal(int winProbability) {
		return winProbability < Threeshold.SHASHIN_HIGH_TAL_THRESHOLD.getValue()
				&& winProbability >= Threeshold.SHASHIN_MIDDLE_HIGH_TAL_THRESHOLD.getValue();
	}

	private static boolean isHighTal(int winProbability) {
		return winProbability >= Threeshold.SHASHIN_HIGH_TAL_THRESHOLD.getValue();
	}
}
