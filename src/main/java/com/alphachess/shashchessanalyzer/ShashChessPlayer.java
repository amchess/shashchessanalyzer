package com.alphachess.shashchessanalyzer;

import static java.lang.String.format;
import static net.andreinc.neatchess.client.breaks.Break.breakOn;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import ictk.boardgame.AmbiguousMoveException;
import ictk.boardgame.History;
import ictk.boardgame.IllegalMoveException;
import ictk.boardgame.OutOfTurnException;
import ictk.boardgame.Result;
import ictk.boardgame.chess.Bishop;
import ictk.boardgame.chess.ChessBoard;
import ictk.boardgame.chess.ChessGame;
import ictk.boardgame.chess.ChessGameInfo;
import ictk.boardgame.chess.ChessMove;
import ictk.boardgame.chess.ChessPlayer;
import ictk.boardgame.chess.ChessResult;
import ictk.boardgame.chess.Knight;
import ictk.boardgame.chess.Queen;
import ictk.boardgame.chess.Rook;
import ictk.boardgame.chess.Square;
import ictk.boardgame.chess.io.ChessAnnotation;
import ictk.boardgame.chess.io.FEN;
import ictk.boardgame.chess.io.PGNWriter;
import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.UCIResponse;
import net.andreinc.neatchess.client.model.Analysis;
import net.andreinc.neatchess.client.model.EngineInfo;
import net.andreinc.neatchess.client.model.Move;
import net.andreinc.neatchess.client.model.option.EngineOption;

public class ShashChessPlayer {
	private static final int CAPABLANCA_THRESHOLD = 9;
	private static final int LOW_THRESHOLD = 20;
	private static final int LOW_MIDDLE_THRESHOLD = 41;
	private static final int MIDDLE_THRESHOLD = 59;
	private static final int HIGH_MIDDLE_THRESHOLD = 93;
	private static final int HIGH_THRESHOLD = 160;
	private static final String HIGH_PETROSIAN = "High Petrosian";
	private static final String HIGH_MIDDLE_PETROSIAN = "Middle High Petrosian";
	private static final String MIDDLE_PETROSIAN = "Middle Petrosian";
	private static final String MIDDLE_LOW_PETROSIAN = "Middle Low Petrosian";
	private static final String LOW_PETROSIAN = "Low Petrosian";
	private static final String CAOS_PETROSIAN_CAPABLANCA = "Caos Petrosian-Capablanca";
	private static final String CAPABLANCA = "Capablanca";
	private static final String CAOS_TAL_CAPABLANCA = "Caos Capablanca-Tal";
	private static final String LOW_TAL = "Low Tal";
	private static final String LOW_MIDDLE_TAL = "Low Middle Tal";
	private static final String MIDDLE_TAL = "Middle Tal";
	private static final String MIDDLE_HIGH_TAL = "Middle High Tal";
	private static final String HIGH_TAL = "High Tal";
	private static final String CAOS_TAL_CAPABLANCA_PETROSIAN = "Caos Tal-Capablanca-Petrosian";
	UCI uci = null;
	private Properties shashChessPlayerProperties;
	private int threadsNumber;
	private int hashSizeMB;
	private int cpuMhz;
	private String syzygyPath;
	private String syzygyProbeDepth;
	private long strongestAverageTimeSecondsForMove;
	private long timeoutSeconds;
	private long timeoutMS;
	private String currentFen;
	private String currentPositionType;
	private String fullDepthThreads;
	private String openingVariety;
	private String persistedLearning;
	private String readOnlyLearning;
	private String mcts;
	private String mCTSThreads;
	private String engineName;
	private String appendGame;
	private String showEngineInfos;
	private FEN fen = new FEN();
	private int halfMoveCounter = 1;
	private ChessGame currentChessGame;
	private int ply;
	private int iterationScore;
	private int iterationDepth;
	private ChessGameInfo currentChessGameInfo = new ChessGameInfo();
	private String ecoCode;
	private String pgnOutputFileName;

	private static final Logger logger = Logger.getLogger(ShashChessPlayer.class.getName());

	public ShashChessPlayer(String[] args) {
		shashChessPlayerProperties = getShashChessAnalyzerProperties(args);
		setInputParameters();
		setTimeoutMS(timeoutSeconds * 1000);
		uci = new UCI(timeoutMS);
	}

	private void setInputParameters() {
		setTimeoutSeconds(Long.parseLong(shashChessPlayerProperties.getProperty("timeoutSeconds")));
		setThreadsNumber(Integer.parseInt(shashChessPlayerProperties.getProperty("threadsNumber")));
		setCpuMhz(Integer.parseInt(shashChessPlayerProperties.getProperty("cpuMhz")));
		setHashSizeMB(Integer.parseInt(shashChessPlayerProperties.getProperty("hashSizeMB")));
		setSyzygyPath(shashChessPlayerProperties.getProperty("syzygyPath"));
		setSyzygyProbeDepth(shashChessPlayerProperties.getProperty("syzygyProbeDepth"));
		setStrongestAverageTimeSecondsForMove(getStrongestAverageTimeSeconds());
		setCurrentFen(shashChessPlayerProperties.getProperty("fen"));
		setFullDepthThreads(shashChessPlayerProperties.getProperty("fullDepthThreads"));
		setOpeningVariety(shashChessPlayerProperties.getProperty("openingVariety"));
		setPersistedLearning(shashChessPlayerProperties.getProperty("persistedLearning"));
		setReadOnlyLearning(shashChessPlayerProperties.getProperty("readOnlyLearning"));
		setMcts(shashChessPlayerProperties.getProperty("mcts"));
		setMCTSThreads(shashChessPlayerProperties.getProperty("mCTSThreads"));
		setEngineName(shashChessPlayerProperties.getProperty("engineName"));
		setAppendGame(shashChessPlayerProperties.getProperty("appendGame"));
		setShowEngineInfos(shashChessPlayerProperties.getProperty("showEngineInfos"));
		setPly(Integer.parseInt(shashChessPlayerProperties.getProperty("ply")));
		setEcoCode(shashChessPlayerProperties.getProperty("ecoCode"));
		setPgnOutputFileName(shashChessPlayerProperties.getProperty("pgnOutputFileName"));
	}

	public long getStrongestAverageTimeSeconds() {
		return (long) (getHashSizeMB() * 512 / (getThreadsNumber() * getCpuMhz()));
	}

	private Properties getShashChessAnalyzerProperties(String[] args) {
		String shashChessAnalyzerProperties = args[0];
		Properties properties = new Properties();
		try {
			File file = new File(shashChessAnalyzerProperties);
			FileInputStream fileInput = new FileInputStream(file);
			properties.load(fileInput);
			fileInput.close();

		} catch (FileNotFoundException e) {
			logger.info(e.getMessage());
			System.exit(0);
		} catch (IOException e) {
			logger.info(e.getMessage());
			System.exit(0);
		}
		return properties;
	}

	public static void main(String[] args) {

		ShashChessPlayer shashChessPlayer = new ShashChessPlayer(args);
		try {
			initShashChess(shashChessPlayer);
			System.out.println(String.join(" ", "Begin playing at",
					Long.toString(shashChessPlayer.getStrongestAverageTimeSeconds() * 2),
					"seconds per move from position"));
			String iterationFen = shashChessPlayer.getCurrentFen().trim();

			if (iterationFen != null) {
				ChessBoard iterationChessBoard = (ChessBoard) shashChessPlayer.getFen().stringToBoard(iterationFen);
				if (iterationChessBoard != null) {
					System.out.println("");
					System.out.println(iterationChessBoard.toString());
					System.out.println("");
					History currentHistory = shashChessPlayer.getCurrentHistory(shashChessPlayer, iterationChessBoard);
					if (currentHistory != null) {
						while ((!iterationChessBoard.isCheckmate()
								&& ((shashChessPlayer.getHalfMoveCounter()) <= shashChessPlayer.getPly()))
								&& (!iterationChessBoard.is50MoveRuleApplicible())) {
							shashChessPlayer.getLan(iterationFen, 1, shashChessPlayer.getHalfMoveCounter(),
									iterationChessBoard.isBlackMove());
							shashChessPlayer.setShashinUciOptions(shashChessPlayer.getCurrentPositionType());
							iterationFen = getStep2Fen(shashChessPlayer, iterationFen, iterationChessBoard,
									currentHistory);
						}

					}
				}
				System.out.println("");
				int finalScore = shashChessPlayer.getIterationScore();
				int finalDepth = shashChessPlayer.getIterationDepth();
				ChessBoard finalChessBoard = (ChessBoard) shashChessPlayer.getFen()
						.stringToBoard(shashChessPlayer.getCurrentFen());
				ChessResult gameResult = getGameResult(finalScore, finalChessBoard);
				shashChessPlayer.getCurrentChessGameInfo().setResult(gameResult);
				System.out.println(shashChessPlayer.getCurrentChessGame().getGameInfo().toString());
				System.out.println(shashChessPlayer.getCurrentChessGame().getHistory().toString());
				writePgn(shashChessPlayer);

			}
		} catch (Exception e) {
			shashChessPlayer.closeShashChess();
			System.out.println("End computation for timeout");
			System.exit(0);
		}
	}

	private static ChessResult getGameResult(int finalScore, ChessBoard finalChessBoard) {
		ChessResult gameResult = new ChessResult(Result.UNDECIDED);
		if ((finalScore > -CAPABLANCA_THRESHOLD) && (finalScore < CAPABLANCA_THRESHOLD)) {
			gameResult.set(ChessResult.DRAW);
		}
		if ((finalScore <= -CAPABLANCA_THRESHOLD) && finalChessBoard.isBlackMove()) {
			gameResult.set(ChessResult.BLACK_WIN);
		}
		if ((finalScore <= -CAPABLANCA_THRESHOLD) && !finalChessBoard.isBlackMove()) {
			gameResult.set(ChessResult.WHITE_WIN);
		}
		if ((finalScore >= CAPABLANCA_THRESHOLD) && finalChessBoard.isBlackMove()) {
			gameResult.set(ChessResult.WHITE_WIN);
		}
		if ((finalScore >= CAPABLANCA_THRESHOLD) && !finalChessBoard.isBlackMove()) {
			gameResult.set(ChessResult.BLACK_WIN);
		}
		return gameResult;
	}

	private static void writePgn(ShashChessPlayer shashChessAnalyzer) {
		PGNWriter pgnWriter = null;
		PrintWriter pw = null;
		try {
			String appendGame=shashChessAnalyzer.getAppendGame();
			pw = new PrintWriter(new FileWriter(shashChessAnalyzer.getPgnOutputFileName(), ((appendGame!=null)&&(appendGame.equalsIgnoreCase("Yes")))?true:false));
			pgnWriter = new PGNWriter(pw);
			pgnWriter.writeGame(shashChessAnalyzer.getCurrentChessGame());
			pw.print("\r\n");
			pw.close();
			pgnWriter.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		shashChessAnalyzer.closeShashChess();
	}

	private History getCurrentHistory(ShashChessPlayer shashChessAnalyzer, ChessBoard iterationChessBoard) {
		shashChessAnalyzer.setCurrentChessGame(getCurrentChessGame(iterationChessBoard));
		ChessGameInfo currentChessGameInfo = getCurrentChessGameInfo(shashChessAnalyzer, iterationChessBoard,
				shashChessAnalyzer.getCurrentChessGame());
		shashChessAnalyzer.getCurrentChessGame().setGameInfo(currentChessGameInfo);
		History currentHistory = shashChessAnalyzer.getCurrentChessGame().getHistory();
		return currentHistory;
	}

	private static ChessGame getCurrentChessGame(ChessBoard iterationChessBoard) {
		ChessGame currentChessGame = new ChessGame();
		currentChessGame.setBoard(iterationChessBoard);
		return currentChessGame;
	}

	private static ChessGameInfo getCurrentChessGameInfo(ShashChessPlayer shashChessPlayer,
			ChessBoard iterationChessBoard, ChessGame currentChessGame) {

		shashChessPlayer.getCurrentChessGameInfo().setBlack(new ChessPlayer(shashChessPlayer.getEngineName()));
		shashChessPlayer.getCurrentChessGameInfo().setWhite(new ChessPlayer(shashChessPlayer.getEngineName()));
		Calendar calendar = Calendar.getInstance();
		Date currentDate = calendar.getTime();
		calendar.setTime(currentDate);
		shashChessPlayer.getCurrentChessGameInfo().setDay(calendar.get(Calendar.DAY_OF_WEEK));
		shashChessPlayer.getCurrentChessGameInfo().setMonth(calendar.get(Calendar.MONTH) + 1);
		shashChessPlayer.getCurrentChessGameInfo().setYear(calendar.get(Calendar.YEAR));
		shashChessPlayer.getCurrentChessGameInfo().setDate(calendar);
		shashChessPlayer.getCurrentChessGameInfo().setSite(shashChessPlayer.getComputerName());
		shashChessPlayer.getCurrentChessGameInfo().setBlackRating(3500);
		shashChessPlayer.getCurrentChessGameInfo().setWhiteRating(3500);
		shashChessPlayer.getCurrentChessGameInfo().setECO(shashChessPlayer.getEcoCode());
		shashChessPlayer.getCurrentChessGameInfo().setEvent("ShashChessPlayer game");
		shashChessPlayer.getCurrentChessGameInfo().setRound("1");
		shashChessPlayer.getCurrentChessGameInfo().setSubRound("");
		shashChessPlayer.getCurrentChessGameInfo().setTimeControlIncrement(0);
		shashChessPlayer.getCurrentChessGameInfo()
				.setTimeControlInitial((int) shashChessPlayer.getStrongestAverageTimeSeconds() * 2);
		return shashChessPlayer.getCurrentChessGameInfo();
	}

	private String getComputerName() {
		Map<String, String> env = System.getenv();
		if (env.containsKey("COMPUTERNAME"))
			return env.get("COMPUTERNAME");
		else if (env.containsKey("HOSTNAME"))
			return env.get("HOSTNAME");
		else
			return "Unknown Computer";
	}

	private static String getStep2Fen(ShashChessPlayer shashChessAnalyzer, String iterationFen,
			ChessBoard iterationChessBoard, History currentHistory)
			throws IllegalMoveException, OutOfTurnException, AmbiguousMoveException {
		String lan2 = shashChessAnalyzer.getLan(iterationFen, 2, shashChessAnalyzer.getHalfMoveCounter(),
				iterationChessBoard.isBlackMove());
		if (lan2 != null) {
			String origSquareStr = lan2.substring(0, 2);
			Square origSquare = iterationChessBoard.getSquare((byte) (origSquareStr.charAt(0) - 96),
					Byte.parseByte(origSquareStr.substring(1)));
			String destSquareStr = lan2.substring(2, 4);
			Square destSquare = iterationChessBoard.getSquare((byte) (destSquareStr.charAt(0) - 96),
					Byte.parseByte(destSquareStr.substring(1)));
			if ((origSquareStr != null) && (origSquare != null) && (destSquareStr != null) && (destSquare != null)) {
				iterationFen = getCurrentChessMove(shashChessAnalyzer, iterationChessBoard, currentHistory, lan2,
						origSquareStr, origSquare, destSquareStr, destSquare);
			}
			shashChessAnalyzer.setHalfMoveCounter(iterationChessBoard.isBlackMove()?shashChessAnalyzer.getHalfMoveCounter():shashChessAnalyzer.getHalfMoveCounter() + 1);
		}
		return iterationFen;
	}

	private static String getCurrentChessMove(ShashChessPlayer shashChessAnalyzer, ChessBoard iterationChessBoard,
			History currentHistory, String lan2, String origSquareStr, Square origSquare, String destSquareStr,
			Square destSquare) throws IllegalMoveException, OutOfTurnException, AmbiguousMoveException {
		String iterationFen=null;
		ChessMove iterationChessMove = null;
		iterationChessMove = getPromotionMove(shashChessAnalyzer, iterationChessBoard, lan2, origSquare, destSquare,
				iterationChessMove);
		iterationChessMove = getCastleMove(shashChessAnalyzer, iterationChessBoard, origSquareStr, destSquareStr,
				iterationChessMove);
		if (iterationChessMove == null) {
			iterationChessMove = new ChessMove(iterationChessBoard, origSquare, destSquare);
		}
		if (iterationChessMove != null) {
			currentHistory.add(iterationChessMove);
		}
		ChessAnnotation iterationChessMoveAnnotation=new ChessAnnotation();
		iterationChessMoveAnnotation.setComment(String.join("",Integer.toString(shashChessAnalyzer.getIterationScore()),"cp"," ",Integer.toString(shashChessAnalyzer.getIterationDepth()),"plies"," ",shashChessAnalyzer.getCurrentPositionType()));
		iterationChessMove.setAnnotation(iterationChessMoveAnnotation);
		shashChessAnalyzer.setCurrentFen(shashChessAnalyzer.getFen().boardToString(iterationChessBoard));
		if(shashChessAnalyzer.getCurrentFen()!=null)
		{
			iterationFen = shashChessAnalyzer.getCurrentFen().trim();
		}
		return iterationFen;
	}

	private static ChessMove getCastleMove(ShashChessPlayer shashChessAnalyzer, ChessBoard iterationChessBoard,
			String origSquareStr, String destSquareStr, ChessMove iterationChessMove) throws IllegalMoveException {
		if (iterationChessMove == null) {
			iterationChessMove = shashChessAnalyzer.getCastleMove(origSquareStr, destSquareStr, iterationChessMove,
					iterationChessBoard);
		}
		return iterationChessMove;
	}

	private static ChessMove getPromotionMove(ShashChessPlayer shashChessAnalyzer, ChessBoard iterationChessBoard,
			String lan2, Square origSquare, Square destSquare, ChessMove iterationChessMove)
			throws IllegalMoveException {
		if (lan2.length() == 5) {
			char promotionUnit = lan2.substring(4, 5).charAt(0);
			iterationChessMove = shashChessAnalyzer.getPromotionMove(origSquare, destSquare, iterationChessBoard,
					promotionUnit);
		}
		return iterationChessMove;
	}

	private ChessMove getPromotionMove(Square origSquare, Square destSquare, ChessBoard chessBoard, char promotionUnit)
			throws IllegalMoveException {
		int promo = 0;
		switch (promotionUnit) {
		case 'q':
			promo = Queen.INDEX;
			break;
		case 'r':
			promo = Rook.INDEX;
			break;
		case 'b':
			promo = Bishop.INDEX;
			break;
		case 'n':
			promo = Knight.INDEX;
			break;
		default:
			break;
		}
		return new ChessMove(chessBoard, origSquare.getFile(), origSquare.getRank(), destSquare.getFile(),
				destSquare.getRank(), promo);
	}

	private ChessMove getCastleMove(String origSquareStr, String destSquareStr, ChessMove currentChessMove,
			ChessBoard iterationChessBoard) throws IllegalMoveException {
		if ((origSquareStr.equals("e1") && destSquareStr.equals("g1"))
				|| (origSquareStr.equals("e8") && destSquareStr.equals("g8"))) {
			currentChessMove = new ChessMove(iterationChessBoard, ChessMove.CASTLE_KINGSIDE);
		}
		if ((origSquareStr.equals("e1") && destSquareStr.equals("c1"))
				|| (origSquareStr.equals("e8") && destSquareStr.equals("c8"))) {
			currentChessMove = new ChessMove(iterationChessBoard, ChessMove.CASTLE_QUEENSIDE);
		}
		return currentChessMove;
	}

	private static void initShashChess(ShashChessPlayer shashChessAnalyzer) {
		shashChessAnalyzer.startShashChess();
		shashChessAnalyzer.setInitialUciOptions();
		String showEngineInfos = shashChessAnalyzer.getShowEngineInfos();
		if ((showEngineInfos != null) && (!showEngineInfos.isEmpty()) && (showEngineInfos.equalsIgnoreCase("yes"))) {
			shashChessAnalyzer.retrieveShashChessInfo();
		}
	}

	private String getLan(String fen, int step, int moveCounter, boolean isBlackMove) {
		long currentAverageTimeMSForMove = strongestAverageTimeSecondsForMove * 1000;
		uci.uciNewGame();
		uci.positionFen(fen);
		String goCommand = "go movetime %d";
		UCIResponse<Analysis> response = uci.command(format(goCommand, (long) currentAverageTimeMSForMove),
				UCI.analysis::process, breakOn("bestmove"), uci.getDefaultTimeout());
		Analysis analysis = response.getResultOrThrow();
		Move bestMove = analysis.getBestMove();
		String lan = bestMove.getLan();
		setIterationScore(((Double) (bestMove.getStrength().getScore() * 100)).intValue());
		setIterationDepth(bestMove.getDepth());
		setCurrentPositionType(getPositionType(iterationScore));
		if (step == 2) {
			System.out.println(
					String.join(" ", (String.join("", Integer.toString(moveCounter), isBlackMove ? "...." : ".")),
							bestMove.getLan(), String.join("", Integer.toString(iterationScore), "cp "),
							String.join("", Integer.toString(iterationDepth), "plies "), getCurrentPositionType()));
		}
		return lan;
	}

	private void setShashinUciOptions(String positionType) {
		switch (positionType) {
		case HIGH_PETROSIAN:
			uci.setOption(HIGH_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			break;
		case HIGH_MIDDLE_PETROSIAN:
			uci.setOption(HIGH_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(MIDDLE_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			break;
		case MIDDLE_PETROSIAN:
			uci.setOption(MIDDLE_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			break;
		case MIDDLE_LOW_PETROSIAN:
			uci.setOption(MIDDLE_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(LOW_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			break;
		case LOW_PETROSIAN:
			uci.setOption(LOW_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			break;
		case CAOS_PETROSIAN_CAPABLANCA:
			uci.setOption(LOW_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(CAPABLANCA, "true", timeoutMS).getResultOrThrow();
			break;
		case CAPABLANCA:
			uci.setOption(CAPABLANCA, "true", timeoutMS).getResultOrThrow();
			break;
		case CAOS_TAL_CAPABLANCA:
			uci.setOption(CAPABLANCA, "true", timeoutMS).getResultOrThrow();
			uci.setOption(LOW_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case LOW_TAL:
			uci.setOption(LOW_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case LOW_MIDDLE_TAL:
			uci.setOption(LOW_TAL, "true", timeoutMS).getResultOrThrow();
			uci.setOption(MIDDLE_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case MIDDLE_TAL:
			uci.setOption(MIDDLE_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case MIDDLE_HIGH_TAL:
			uci.setOption(MIDDLE_TAL, "true", timeoutMS).getResultOrThrow();
			uci.setOption(HIGH_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case HIGH_TAL:
			uci.setOption(HIGH_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		case CAOS_TAL_CAPABLANCA_PETROSIAN:
			uci.setOption(HIGH_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(MIDDLE_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(LOW_PETROSIAN, "true", timeoutMS).getResultOrThrow();
			uci.setOption(CAPABLANCA, "true", timeoutMS).getResultOrThrow();
			uci.setOption(LOW_TAL, "true", timeoutMS).getResultOrThrow();
			uci.setOption(LOW_MIDDLE_TAL, "true", timeoutMS).getResultOrThrow();
			uci.setOption(MIDDLE_TAL, "true", timeoutMS).getResultOrThrow();
			uci.setOption(HIGH_TAL, "true", timeoutMS).getResultOrThrow();
			break;
		default:
			break;
		}

	}

	private String getPositionType(int score) {
		if (score <= -HIGH_THRESHOLD) {
			return HIGH_PETROSIAN;
		}
		if ((score > -HIGH_THRESHOLD) && (score <= -HIGH_MIDDLE_THRESHOLD)) {
			return HIGH_MIDDLE_PETROSIAN;
		}
		if ((score > -HIGH_MIDDLE_THRESHOLD) && (score <= -MIDDLE_THRESHOLD)) {
			return MIDDLE_PETROSIAN;
		}
		if ((score > -MIDDLE_THRESHOLD) && (score <= -LOW_MIDDLE_THRESHOLD)) {
			return MIDDLE_LOW_PETROSIAN;
		}
		if ((score > -LOW_MIDDLE_THRESHOLD) && (score <= -LOW_THRESHOLD)) {
			return LOW_PETROSIAN;
		}
		if ((score > -LOW_THRESHOLD) && (score <= -CAPABLANCA_THRESHOLD)) {
			return CAOS_PETROSIAN_CAPABLANCA;
		}
		if ((score > -CAPABLANCA_THRESHOLD) && (score < CAPABLANCA_THRESHOLD)) {
			return CAPABLANCA;
		}
		if ((score >= CAPABLANCA_THRESHOLD) && (score < LOW_THRESHOLD)) {
			return CAOS_TAL_CAPABLANCA;
		}
		if ((score >= LOW_THRESHOLD) && (score < LOW_MIDDLE_THRESHOLD)) {
			return LOW_TAL;
		}
		if ((score >= LOW_MIDDLE_THRESHOLD) && (score < MIDDLE_THRESHOLD)) {
			return LOW_MIDDLE_TAL;
		}
		if ((score >= MIDDLE_THRESHOLD) && (score < HIGH_MIDDLE_THRESHOLD)) {
			return MIDDLE_TAL;
		}
		if ((score >= HIGH_MIDDLE_THRESHOLD) && (score < HIGH_THRESHOLD)) {
			return MIDDLE_HIGH_TAL;
		}
		if (score >= HIGH_THRESHOLD) {
			return HIGH_TAL;
		}
		return CAOS_TAL_CAPABLANCA_PETROSIAN;
	}

	private void setInitialUciOptions() {
		try {
			uci.setOption("Threads", Integer.toString(threadsNumber), timeoutMS).getResultOrThrow();
			uci.setOption("Hash", Integer.toString(hashSizeMB), timeoutMS).getResultOrThrow();
			uci.setOption("SyzygyPath", syzygyPath, timeoutMS).getResultOrThrow();
			uci.setOption("SyzygyProbeDepth", syzygyProbeDepth, timeoutMS).getResultOrThrow();
			uci.setOption("Full depth threads", fullDepthThreads, timeoutMS).getResultOrThrow();
			uci.setOption("Opening variety", openingVariety, timeoutMS).getResultOrThrow();
			uci.setOption("Persisted learning", persistedLearning, timeoutMS).getResultOrThrow();
			uci.setOption("Read only learning", readOnlyLearning, timeoutMS).getResultOrThrow();
			uci.setOption("MCTS", mcts, timeoutMS).getResultOrThrow();
			uci.setOption("MCTSThreads", mCTSThreads, timeoutMS).getResultOrThrow();
		} catch (Exception e) {
			closeShashChess();
			System.out.println("Impossible to setup uci options");
			System.exit(0);
		}
	}

	@SuppressWarnings("rawtypes")
	private void retrieveShashChessInfo() {
		UCIResponse<EngineInfo> response = uci.getEngineInfo();
		if (response.success()) {

			// Engine name
			EngineInfo engineInfo = response.getResult();
			System.out.println(String.join("", "Engine name:" + engineInfo.getName()));

			// Supported engine options
			System.out.println("Supported engine options:");
			Map<String, EngineOption> engineOptions = engineInfo.getOptions();
			engineOptions.forEach((key, value) -> {
				System.out.println(String.join("", "\t", key));
				System.out.println(String.join("", "\t\t", value.toString()));
			});
		}
	}

	private void closeShashChess() {
		uci.close();
		System.out.println("Engine closed");
		System.exit(0);
	}

	private void startShashChess() {
		String engineNameWithExtension = String.join("",
				(System.getProperty("os.name").contains("Windows") ? engineName : String.join("", "./", engineName)),
				(System.getProperty("os.name").contains("Windows") ? ".exe" : ""));
		uci.start(engineNameWithExtension);
	}

	public UCI getUci() {
		return uci;
	}

	public void setUci(UCI uci) {
		this.uci = uci;
	}

	public int getHashSizeMB() {
		return hashSizeMB;
	}

	public void setHashSizeMB(int hashSizeMB) {
		this.hashSizeMB = hashSizeMB;
	}

	public int getCpuMhz() {
		return cpuMhz;
	}

	public void setCpuMhz(int cpuMhz) {
		this.cpuMhz = cpuMhz;
	}

	public String getSyzygyPath() {
		return syzygyPath;
	}

	public void setSyzygyPath(String syzygyPath) {
		this.syzygyPath = syzygyPath;
	}

	public String getSyzygyProbeDepth() {
		return syzygyProbeDepth;
	}

	public void setSyzygyProbeDepth(String syzygyProbeDepth) {
		this.syzygyProbeDepth = syzygyProbeDepth;
	}

	public Properties getShashChessAnalyzerProperties() {
		return shashChessPlayerProperties;
	}

	public long getTimeoutMS() {
		return timeoutMS;
	}

	public void setTimeoutMS(long timeoutMS) {
		this.timeoutMS = timeoutMS;
	}

	public int getThreadsNumber() {
		return threadsNumber;
	}

	public void setThreadsNumber(int threadsNumber) {
		this.threadsNumber = threadsNumber;
	}

	public long getStrongestAverageTimeSecondsForMove() {
		return strongestAverageTimeSecondsForMove;
	}

	public void setStrongestAverageTimeSecondsForMove(long strongestAverageTimeSecondsForMove) {
		this.strongestAverageTimeSecondsForMove = strongestAverageTimeSecondsForMove;
	}

	public String getCurrentFen() {
		return currentFen;
	}

	public long getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(long timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public void setCurrentFen(String fen) {
		this.currentFen = fen;
	}

	public String getFullDepthThreads() {
		return fullDepthThreads;
	}

	public String getOpeningVariety() {
		return openingVariety;
	}

	public String getPersistedLearning() {
		return persistedLearning;
	}

	public String getReadOnlyLearning() {
		return readOnlyLearning;
	}

	public String getMcts() {
		return mcts;
	}

	public String getMCTSThreads() {
		return mCTSThreads;
	}

	public void setFullDepthThreads(String fullDepthThreads) {
		this.fullDepthThreads = fullDepthThreads;
	}

	public void setOpeningVariety(String openingVariety) {
		this.openingVariety = openingVariety;
	}

	public void setPersistedLearning(String persistedLearning) {
		this.persistedLearning = persistedLearning;
	}

	public void setReadOnlyLearning(String readOnlyLearning) {
		this.readOnlyLearning = readOnlyLearning;
	}

	public void setMcts(String mcts) {
		this.mcts = mcts;
	}

	public void setMCTSThreads(String mCTSThreads) {
		this.mCTSThreads = mCTSThreads;
	}

	public String getEngineName() {
		return engineName;
	}

	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

	public String getShowEngineInfos() {
		return showEngineInfos;
	}

	public void setShowEngineInfos(String showEngineInfos) {
		this.showEngineInfos = showEngineInfos;
	}

	public String getCurrentPositionType() {
		return currentPositionType;
	}

	public void setCurrentPositionType(String currentPositionType) {
		this.currentPositionType = currentPositionType;
	}

	public FEN getFen() {
		return fen;
	}

	public void setFen(FEN fen) {
		this.fen = fen;
	}

	public int getHalfMoveCounter() {
		return halfMoveCounter;
	}

	public void setHalfMoveCounter(int halfmoveCounter) {
		this.halfMoveCounter = halfmoveCounter;
	}

	public ChessGame getCurrentChessGame() {
		return currentChessGame;
	}

	public void setCurrentChessGame(ChessGame currentChessGame) {
		this.currentChessGame = currentChessGame;
	}

	public int getPly() {
		return ply;
	}

	public void setPly(int ply) {
		this.ply = ply;
	}

	public int getIterationScore() {
		return iterationScore;
	}

	public void setIterationScore(int iterationScore) {
		this.iterationScore = iterationScore;
	}

	public ChessGameInfo getCurrentChessGameInfo() {
		return currentChessGameInfo;
	}

	public void setCurrentChessGameInfo(ChessGameInfo currentChessGameInfo) {
		this.currentChessGameInfo = currentChessGameInfo;
	}

	public String getEcoCode() {
		return ecoCode;
	}

	public void setEcoCode(String ecoCode) {
		this.ecoCode = ecoCode;
	}

	public String getPgnOutputFileName() {
		return pgnOutputFileName;
	}

	public void setPgnOutputFileName(String pgnOutputFileName) {
		this.pgnOutputFileName = pgnOutputFileName;
	}

	public int getIterationDepth() {
		return iterationDepth;
	}

	public void setIterationDepth(int iterationDepth) {
		this.iterationDepth = iterationDepth;
	}

	public String getAppendGame() {
		return appendGame;
	}

	public void setAppendGame(String appendGame) {
		this.appendGame = appendGame;
	}
}
