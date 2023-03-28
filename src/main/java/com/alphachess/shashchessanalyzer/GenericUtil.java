package com.alphachess.shashchessanalyzer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import ictk.boardgame.chess.ChessMove;
import ictk.boardgame.chess.ChessPiece;
import ictk.boardgame.chess.Square;

public class GenericUtil {
	private GenericUtil() {
		throw new IllegalStateException("Utility class");
	}

	public static final int MAX_PLY = 246;
	public static final int VALUE_DRAW = 0;
	public static final int KING_POS = 0;
	public static final int ROOK_POS = 1;
	public static final int KNIGHT_POS = 2;
	public static final int BISHOP_POS = 3;
	public static final int QUEEN_POS = 4;
	public static final int PAWN_POS = 5;
	private static final java.util.logging.Logger chessLearnerLogger = java.util.logging.Logger
			.getLogger("chessLearner.log");

	public enum KingPosition {
		K(1), C(2), Q(4);

		private int value;

		private KingPosition(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public static File getFile(File file, int fileIndex) {
		String absolutePath = file.getAbsolutePath();
		String newAbsolutePath = absolutePath.replace(".", fileIndex + ".");
		return new File(newAbsolutePath);
	}

	public static byte[] intToByteArray(int data) {
		byte[] result = new byte[4];
		result[0] = (byte) ((data & 0xFF000000) >> 24);
		result[1] = (byte) ((data & 0x00FF0000) >> 16);
		result[2] = (byte) ((data & 0x0000FF00) >> 8);
		result[3] = (byte) ((data & 0x000000FF) >> 0);
		return result;
	}

	public static String getAlgebricMove(int moveInt) {
		int fromSquareInt = getFromSquare(moveInt);
		int toSquareInt = getToSquare(moveInt);
		String fromSquareAlgebricNotation = getAlgebricSquare(fromSquareInt);
		String toSquareAlgebricNotation = getAlgebricSquare(toSquareInt);
		return fromSquareAlgebricNotation.concat(toSquareAlgebricNotation);
	}

	public static String getAlgebricSquare(int squareInt) {
		return Character.toString((char) ((squareInt % 8) + 97)).concat(Integer.toString((squareInt / 8) + 1));
	}

	public static int getIntSquare(String algebricSquare) {
		Square square = new Square((byte) (algebricSquare.charAt(0) - 96), Byte.parseByte(algebricSquare.substring(1)));
		return getSquareInt(square);
	}

	public static int getFromSquare(int move) {
		return ((move >> 6) & 0x3F);
	}

	public static Square getSquare(int intSquare) {
		String algebricSquare = getAlgebricSquare(intSquare);
		return new Square((byte) (algebricSquare.charAt(0) - 96), Byte.parseByte(algebricSquare.substring(1)));
	}

	public static int getToSquare(int move) {
		return (move & 0x3F);
	}

	public static int getMoveInt(int fromSquare, int toSquare) {
		return (64 * fromSquare + toSquare);
	}

	public static int getMoveInt(Square fromSquare, Square toSquare) {
		return getMoveInt(getSquareInt(fromSquare), getSquareInt(toSquare));
	}

	public static int getSquareInt(Square square) {
		return (8 * square.getRank() - 9 + square.getFile());
	}

	public static int getIntMove(String algebricMove) {
		int fromColStr = algebricMove.charAt(0);
		int fromColInt = fromColStr - 96;
		int fromRowInt = Integer.parseInt(algebricMove.substring(1, 2));
		int fromSquareInt = 8 * fromRowInt + fromColInt - 9;
		int toColStr = algebricMove.charAt(2);
		int toColInt = toColStr - 96;
		int toRowInt = Integer.parseInt(algebricMove.substring(3));
		int toSquareInt = 8 * toRowInt + toColInt - 9;
		return getMoveInt(fromSquareInt, toSquareInt);
	}

	public static int getMoveInt(ChessMove chessMove) {
		if (chessMove != null) {
			Square fromSquare = chessMove.getOrigin();
			Square toSquare = chessMove.getDestination();
			if ((fromSquare != null) && (toSquare != null)) {
				return getMoveInt(fromSquare, toSquare);
			}
		}
		return 0;
	}

	public static int getChessPieceIndex(ChessPiece chessPiece) {
		if (chessPiece.isKing()) {
			return KING_POS;
		}
		if (chessPiece.isRook()) {
			return ROOK_POS;
		}
		if (chessPiece.isKnight()) {
			return KNIGHT_POS;
		}
		if (chessPiece.isBishop()) {
			return BISHOP_POS;
		}
		if (chessPiece.isQueen()) {
			return QUEEN_POS;
		}
		return PAWN_POS;
	}

	@SafeVarargs
	public static <T> List<T> asList(T... elements) {
		if (elements.length == 0)
			return new ArrayList<>();
		return new ArrayList<>(Arrays.asList(elements));
	}

	public static Properties getProperties(String propertiesResourceFileName) {
		Properties prop = new Properties();
		try (InputStream inputStream = ClassLoader.getSystemResourceAsStream(propertiesResourceFileName)) {
			prop.load(inputStream);
		} catch (IOException ex) {
			chessLearnerLogger.log(Level.SEVERE, ex.getMessage(), ex);
		}
		return prop;
	}
	public static double clamp(double val, double min, double max) {
	    return Math.max(min, Math.min(max, val));
	}

}
