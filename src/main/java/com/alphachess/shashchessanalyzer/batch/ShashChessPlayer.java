package com.alphachess.shashchessanalyzer.batch;

import static java.lang.String.format;
import static net.andreinc.neatchess.client.breaks.Break.breakOn;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.alphachess.shashchessanalyzer.WinProbabilityByMaterial;
import com.alphachess.shashchessanalyzer.WinProbabilityByMaterial.Range;
import com.alphachess.shashchessanalyzer.WinProbabilityByMaterial.RangeDescription;

import ictk.boardgame.AmbiguousMoveException;
import ictk.boardgame.History;
import ictk.boardgame.IllegalMoveException;
import ictk.boardgame.Result;
import ictk.boardgame.chess.Bishop;
import ictk.boardgame.chess.ChessBoard;
import ictk.boardgame.chess.ChessGame;
import ictk.boardgame.chess.ChessGameInfo;
import ictk.boardgame.chess.ChessMove;
import ictk.boardgame.chess.ChessPiece;
import ictk.boardgame.chess.ChessPlayer;
import ictk.boardgame.chess.ChessResult;
import ictk.boardgame.chess.Knight;
import ictk.boardgame.chess.Queen;
import ictk.boardgame.chess.Rook;
import ictk.boardgame.chess.io.ChessAnnotation;
import ictk.boardgame.chess.io.FEN;
import ictk.boardgame.chess.io.PGNReader;
import ictk.boardgame.chess.io.PGNWriter;
import ictk.boardgame.io.InvalidGameFormatException;
import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.UCIResponse;
import net.andreinc.neatchess.client.model.Analysis;
import net.andreinc.neatchess.client.model.BestMove;
import net.andreinc.neatchess.client.model.EngineInfo;
import net.andreinc.neatchess.client.model.Move;
import net.andreinc.neatchess.client.model.option.EngineOption;

public class ShashChessPlayer {
	private static final String LIVE_BOOK_PROXY_URL = "LiveBook Proxy Url";
	private static final String START_POS = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	private static final String CHESSDB_TABLEBASE = "ChessDB Tablebase";
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
	private String variety;
	private String persistedLearning;
	private String readOnlyLearning;
	private String chessDBTablebase;
	private String livebookProxyUrl;
	private String mcts;
	private String mCTSThreads;
	private String engineName;
	private String appendGame;
	private String showEngineInfos;
	private FEN fen = new FEN();
	private int moveCounter = 1;
	private ChessGame currentChessGame;
	private ChessGame currentInputChessGame;
	private int maxMovesNumber;
	private int iterationScore;
	private int iterationDepth;
	private ChessGameInfo currentChessGameInfo = new ChessGameInfo();
	private String ecoCode;
	private String pgnOutputFileName;
	private String inputGamesPgn;
	private Date dataInizioElaborazionePrincipale;
	private PGNReader pgnReader;
	private FileReader gamesFileReader;
	private BufferedReader gamesInputBufferedReader;
	private long numeroPartiteTotali = 0;
	private long numeroPartiteInserite = 0;
	private long numeroPartiteInFormatoScorretto = 0;
	private long partitaCorrente = 0;
	private int gamesMoveFromEco;
	private int semiMoveNumber;
	private PGNWriter pgnWriter = null;
	private PrintWriter pw = null;
	private boolean blackCastleable = true;
	private boolean whiteCastleable = true;

	private static final Logger logger = Logger.getLogger(ShashChessPlayer.class.getName());

	public ShashChessPlayer(String[] args) {
		shashChessPlayerProperties = getShashChessPlayerProperties(args);
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
		setCurrentFen(
				shashChessPlayerProperties.getProperty("fen") != null ? shashChessPlayerProperties.getProperty("fen")
						: "");
		setFullDepthThreads(shashChessPlayerProperties.getProperty("fullDepthThreads"));
		setVariety(shashChessPlayerProperties.getProperty("variety"));
		setPersistedLearning(shashChessPlayerProperties.getProperty("persistedLearning"));
		setReadOnlyLearning(shashChessPlayerProperties.getProperty("readOnlyLearning"));
		setLivebookProxyUrl(shashChessPlayerProperties.getProperty("livebookProxyUrl"));
		setChessDBTablebase(shashChessPlayerProperties.getProperty("chessDBTablebase"));
		setMcts(shashChessPlayerProperties.getProperty("mcts"));
		setMCTSThreads(shashChessPlayerProperties.getProperty("mCTSThreads"));
		setEngineName(shashChessPlayerProperties.getProperty("engineName"));
		setAppendGame(shashChessPlayerProperties.getProperty("appendGame"));
		setShowEngineInfos(shashChessPlayerProperties.getProperty("showEngineInfos"));
		setMaxMovesNumber(Integer.parseInt(shashChessPlayerProperties.getProperty("maxMovesNumber")));
		setEcoCode(shashChessPlayerProperties.getProperty("ecoCode"));
		setPgnOutputFileName(shashChessPlayerProperties.getProperty("pgnOutputFileName"));
		setInputGamesPgn(shashChessPlayerProperties.getProperty("inputGamesPgn"));
		setGamesMoveFromEco(Integer.parseInt(shashChessPlayerProperties.getProperty("gamesMoveFromEco")));
	}

	public long getStrongestAverageTimeSeconds() {
		return (getHashSizeMB() * 512 / (getThreadsNumber() * getCpuMhz()));
	}

	private Properties getShashChessPlayerProperties(String[] args) {
		String shashChessAnalyzerProperties = args[0];
		Properties properties = new Properties();

		File file = new File(shashChessAnalyzerProperties);
		try (FileInputStream fileInput = new FileInputStream(file)) {
			properties.load(fileInput);
		} catch (IOException e) {
			logger.info(e.getMessage());
		}

		return properties;
	}

	public static void main(String[] args) {

		ShashChessPlayer shashChessPlayer = new ShashChessPlayer(args);
		try {
			shashChessPlayer.setPgnWriter();
			shashChessPlayer.initShashChess();
			String beginPlayingMsg = String.join(" ", "Begin playing at",
					Long.toString(shashChessPlayer.getStrongestAverageTimeSeconds() * 2),
					"seconds per move from position");
			logger.info(beginPlayingMsg);
			String iterationFen = shashChessPlayer.getCurrentFen().trim();
			String inputGamesPgn = shashChessPlayer.getInputGamesPgn();
			if (inputGamesPgn != null && !inputGamesPgn.isEmpty()) {
				shashChessPlayer.playFromPgnInput();
			} else {
				shashChessPlayer.playFromIterationFen(iterationFen);
			}
		} catch (Exception e) {
			shashChessPlayer.closeWrite();
			shashChessPlayer.closeShashChess();
			logger.info("End computation for timeout");
			System.exit(0);
		}
	}

	private void playFromPgnInput() throws IllegalMoveException, AmbiguousMoveException {
		setDataInizioElaborazionePrincipale(new Date());
		int currentInputGameNumber = 0;
		try {
			pgnReader = getPGNReaderFromFile();
			ChessGame currentInputGame = getCurrentInputGame();
			while (currentInputGame != null) {
				String gameInfoMsg = currentInputGame.getGameInfo().toString();
				logger.info(gameInfoMsg);
				String historyMsg = currentInputGame.getHistory().toString();
				logger.info(historyMsg);
				logger.info("");
				setCurrentInputChessGame(currentInputGame);
				currentInputGameNumber++;
				String currentInputGameMsg = "Current input game: " + currentInputGameNumber;
				logger.info(currentInputGameMsg);
				setCurrentChessGameFromInput(currentInputGame);
				History currentInputHistory = getCurrentInputHistory(currentInputGame);
				setMoveCounter(
						(((semiMoveNumber + 1) % 2 != 0) ? (int) Math.floor((double) (semiMoveNumber + 1) / (double) 2)
								: ((semiMoveNumber + 1) / 2)));
				ChessGameInfo currentInputChessGameInfo = (ChessGameInfo) (currentInputGame.getGameInfo());
				String currentEcoCode = currentInputChessGameInfo.getECO() != null ? currentInputChessGameInfo.getECO()
						: currentInputGame.getGameInfo().getSite();
				setEcoCode(currentEcoCode);
				setMaxMovesNumber(getMoveCounter() + getGamesMoveFromEco());
				playFromIterationFen(currentInputHistory);
				currentInputGame = getCurrentInputGame();
			}
			closeAll();
		} catch (IllegalMoveException | AmbiguousMoveException | IOException e) {
			logger.info(e.getMessage());
		}

	}

	private void closeAll() throws IOException {
		closeWrite();
		closeShashChess();
		logger.info("Engine closed");
		System.exit(0);
		closeGamesReader();
	}

	private void playFromIterationFen(History currentInputHistory)
			throws IOException, IllegalMoveException, AmbiguousMoveException {
		ChessMove currentMove = (ChessMove) currentInputHistory.getCurrentMove();
		String iterationFen = currentMove != null ? getFen().boardToString(currentMove.getBoard()) : START_POS;
		playFromIterationFen(iterationFen);
	}

	private History getCurrentInputHistory(ChessGame currentInputGame) {
		History currentInputHistory = currentInputGame.getHistory();
		currentInputHistory.rewind();
		semiMoveNumber = 0;
		while (currentInputHistory.hasNext()) {
			currentInputHistory.next();
			semiMoveNumber++;
		}
		currentInputHistory.goToEnd();
		getCurrentChessGame().setHistory(currentInputHistory);
		return currentInputHistory;
	}

	private void setCurrentChessGameFromInput(ChessGame currentInputGame) {
		setCurrentChessGame(new ChessGame());
		getCurrentChessGame().setGameInfo(getChessGameInfo());
		getCurrentChessGame().setBoard(currentInputGame.getBoard());
	}

	private void playFromIterationFen(String iterationFen)
			throws IOException, IllegalMoveException, AmbiguousMoveException {
		if (iterationFen != null && !iterationFen.isEmpty()) {
			ChessBoard iterationChessBoard = (ChessBoard) getFen().stringToBoard(iterationFen);
			if (iterationChessBoard != null) {
				if (inputGamesPgn == null) {
					logger.info("");
					String iterationChessBoardMsg = iterationChessBoard.toString();
					logger.info(iterationChessBoardMsg);
					logger.info("");
				}
				logger.info("Starting self play");
				History currentHistory = getCurrentHistory(iterationChessBoard);
				if (currentHistory != null) {
					while ((!iterationChessBoard.isCheckmate() && (getSemiMoveNumber() < getMaxMovesNumber() * 2))
							&& (!iterationChessBoard.is50MoveRuleApplicible())) {
						doStep(iterationFen, 1, iterationChessBoard.isBlackMove());
						setShashinUciOptions(getCurrentPositionType());
						iterationFen = getStep2Fen(iterationFen, iterationChessBoard, currentHistory);
						restartShashChess();
					}
				}
			}
			writeCurrentGame();
		}
	}

	private void restartShashChess() {
		closeShashChess();
		initShashChess();
	}

	private void writeCurrentGame() {
		logger.info("");
		int finalScore = getIterationScore();
		String currentGameFen = getCurrentFen().trim();
		ChessResult gameResult = getGameResult(finalScore, currentGameFen);
		getCurrentChessGameInfo().setResult(gameResult);
		String gameInfoMsg = getCurrentChessGame().getGameInfo().toString();
		logger.info(gameInfoMsg);
		String historyMsg = getCurrentChessGame().getHistory().toString();
		logger.info(historyMsg);
		writePgn();
	}

	private void closeGamesReader() throws IOException {
		pgnReader.close();
		gamesFileReader.close();
		gamesInputBufferedReader.close();
	}

	private ChessGame getCurrentInputGame() {
		ChessGame currentInputGame = null;
		while (currentInputGame == null) {
			try {
				currentInputGame = (ChessGame) pgnReader.readGame();
				if (currentInputGame != null) {
					numeroPartiteTotali++;
					String loadedGameMsg = String.join("", "Loaded game: ", Long.toString(numeroPartiteTotali));
					logger.info(loadedGameMsg);
				} else {
					return null;
				}

			} catch (InvalidGameFormatException | IllegalMoveException | AmbiguousMoveException ex) {
				numeroPartiteInFormatoScorretto++;
				logger.info(String.join("", "Game ", Long.toString(numeroPartiteTotali + 1), " has an incorrect pgn."));
			} catch (IOException ioEx) {
				return null;
			}
		}
		return currentInputGame;
	}

	private PGNReader getPGNReaderFromFile() throws FileNotFoundException {
		gamesFileReader = new FileReader(inputGamesPgn.trim());
		gamesInputBufferedReader = new BufferedReader(gamesFileReader);
		pgnReader = new PGNReader(gamesInputBufferedReader);
		return pgnReader;
	}

	private ChessResult getGameResult(int finalScore, String finalFen) {
		ChessBoard finalChessBoard = null;
		try {
			finalChessBoard = (ChessBoard) fen.stringToBoard(finalFen);
		} catch (IOException e) {
			logger.info(e.getMessage());
		}
		int winProbability = WinProbabilityByMaterial.getWinProbabilityFromScore(finalScore, finalFen);
		int rangeValue = WinProbabilityByMaterial.getRange(winProbability);
		ChessResult gameResult = new ChessResult(Result.UNDECIDED);

		if (rangeValue == Range.SHASHIN_POSITION_CAPABLANCA.getValue()) {
			gameResult.set(ChessResult.DRAW);
		}
		if (finalChessBoard != null) {
			if (rangeValue < Range.SHASHIN_POSITION_CAPABLANCA.getValue() && finalChessBoard.isBlackMove()) {
				gameResult.set(ChessResult.BLACK_WIN);
			}
			if (rangeValue < Range.SHASHIN_POSITION_CAPABLANCA.getValue() && !finalChessBoard.isBlackMove()) {
				gameResult.set(ChessResult.WHITE_WIN);
			}
			if ((rangeValue > Range.SHASHIN_POSITION_CAPABLANCA.getValue()) && finalChessBoard.isBlackMove()) {
				gameResult.set(ChessResult.WHITE_WIN);
			}
			if ((finalScore > Range.SHASHIN_POSITION_CAPABLANCA.getValue()) && !finalChessBoard.isBlackMove()) {
				gameResult.set(ChessResult.BLACK_WIN);
			}
		}
		return gameResult;
	}

	private History getCurrentHistory(ChessBoard iterationChessBoard) {
		setCurrentChessGame(getCurrentChessGame(iterationChessBoard));
		ChessGameInfo currentistoryChessGameInfo = getChessGameInfo();
		if (getCurrentInputChessGame() == null) {
			getCurrentChessGame().setGameInfo(currentistoryChessGameInfo);
		}
		return getCurrentChessGame().getHistory();
	}

	private ChessGame getCurrentChessGame(ChessBoard iterationChessBoard) {
		ChessGame currentIterationChessGame = (getCurrentInputChessGame() == null) ? new ChessGame()
				: getCurrentChessGame();
		if (getCurrentInputChessGame() == null) {
			currentIterationChessGame.setBoard(iterationChessBoard);
		}
		return currentIterationChessGame;
	}

	private ChessGameInfo getChessGameInfo() {
		getCurrentChessGameInfo().setBlack(new ChessPlayer(getEngineName()));
		getCurrentChessGameInfo().setWhite(new ChessPlayer(getEngineName()));
		Calendar calendar = Calendar.getInstance();
		Date currentDate = calendar.getTime();
		calendar.setTime(currentDate);
		getCurrentChessGameInfo().setDay(calendar.get(Calendar.DAY_OF_WEEK));
		getCurrentChessGameInfo().setMonth(calendar.get(Calendar.MONTH) + 1);
		getCurrentChessGameInfo().setYear(calendar.get(Calendar.YEAR));
		getCurrentChessGameInfo().setDate(calendar);
		getCurrentChessGameInfo().setSite(getComputerName());
		getCurrentChessGameInfo().setBlackRating(3500);
		getCurrentChessGameInfo().setWhiteRating(3500);
		getCurrentChessGameInfo().setECO(getEcoCode());
		getCurrentChessGameInfo().setEvent((inputGamesPgn != null) ? "Games from pgn" : "Game from fen");
		getCurrentChessGameInfo().setRound("1");
		getCurrentChessGameInfo().setSubRound("");
		getCurrentChessGameInfo().setTimeControlIncrement(0);
		getCurrentChessGameInfo().setTimeControlInitial((int) getStrongestAverageTimeSeconds() * 2);
		return getCurrentChessGameInfo();
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

	private String getStep2Fen(String iterationFen, ChessBoard iterationChessBoard, History currentHistory)
			throws IllegalMoveException, AmbiguousMoveException {
		String lan = doStep(iterationFen, 2, iterationChessBoard.isBlackMove());
		if (lan != null) {
			iterationFen = getCurrentIterationFen(iterationChessBoard, currentHistory, lan);
		}

		return iterationFen;
	}

	private String getCurrentIterationFen(ChessBoard iterationChessBoard, History currentHistory, String lan)
			throws IllegalMoveException, AmbiguousMoveException {
		String moveOriginSquareCoordinates = lan.substring(0, 2);
		String moveDestinationSquareCoordinates = lan.substring(2, 4);
		byte moveOriginSquareFile = (byte) (moveOriginSquareCoordinates.charAt(0) - 96);
		byte moveOriginSquareRank = Byte.parseByte(moveOriginSquareCoordinates.substring(1));
		byte moveDestinationSquareFile = (byte) (moveDestinationSquareCoordinates.charAt(0) - 96);
		byte moveDestinationSquareRank = Byte.parseByte(moveDestinationSquareCoordinates.substring(1));
		String iterationFen = null;

		ChessMove iterationChessMove = null;
		iterationChessMove = getPromotionChessMove(iterationChessBoard, lan, moveOriginSquareFile, moveOriginSquareRank,
				moveDestinationSquareFile, moveDestinationSquareRank, iterationChessMove);
		if (iterationChessMove == null) {
			iterationChessMove = getCastleChessMove(iterationChessBoard, moveOriginSquareCoordinates,
					moveDestinationSquareCoordinates, iterationChessMove);
		}
		if (iterationChessMove == null) {
			iterationChessMove = new ChessMove(iterationChessBoard, moveOriginSquareFile, moveOriginSquareRank,
					moveDestinationSquareFile, moveDestinationSquareRank);
		}
		currentHistory.add(iterationChessMove);
		semiMoveNumber++;
		setMoveCounter((((semiMoveNumber + 1) % 2 != 0) ? (int) Math.floor((double) (semiMoveNumber + 1) / (double) 2)
				: ((semiMoveNumber + 1) / 2)));
		ChessAnnotation iterationChessMoveAnnotation = new ChessAnnotation();
		iterationFen = new FEN().boardToString(iterationChessBoard);
		iterationChessMoveAnnotation.setComment(
				String.join("", Integer.toString(getIterationScore()), ";", Integer.toString(getIterationDepth()), ";",
						Integer.toString(WinProbabilityByMaterial.getWinProbabilityFromScore(getIterationScore(),iterationFen)),
						";", getAbbreviatePositionType(getCurrentPositionType())));
		iterationChessMove.setAnnotation(iterationChessMoveAnnotation);

		ChessPiece iterationChessMoveUnit = iterationChessMove.getChessPiece();
		setCasteable(iterationChessBoard, iterationChessMove, iterationChessMoveUnit);

		setCurrentFen(getFen().boardToString(iterationChessBoard));
		if (getCurrentFen() != null) {
			iterationFen = getCurrentFen().trim();
		}
		return iterationFen;
	}

	private ChessMove getPromotionChessMove(ChessBoard iterationChessBoard, String lan, byte moveOriginSquareFile,
			byte moveOriginSquareRank, byte moveDestinationSquareFile, byte moveDestinationSquareRank,
			ChessMove chessMove) throws IllegalMoveException {
		if (lan.length() == 5) {
			char promotionUnit = lan.substring(4, 5).charAt(0);
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
			chessMove = new ChessMove(iterationChessBoard, moveOriginSquareFile, moveOriginSquareRank,
					moveDestinationSquareFile, moveDestinationSquareRank, promo);
		}
		return chessMove;
	}

	private void setCasteable(ChessBoard iterationChessBoard, ChessMove iterationChessMove,
			ChessPiece iterationChessMoveUnit) {
		boolean isBlackIterationChessMoveUnit = iterationChessMoveUnit.isBlack();
		setCasteableWhite(iterationChessBoard, iterationChessMove, iterationChessMoveUnit,
				isBlackIterationChessMoveUnit);
		setCasteableBlack(iterationChessBoard, iterationChessMove, iterationChessMoveUnit,
				isBlackIterationChessMoveUnit);
	}

	private void setCasteableWhite(ChessBoard iterationChessBoard, ChessMove iterationChessMove,
			ChessPiece iterationChessMoveUnit, boolean isBlackIterationChessMoveUnit) {
		if (isWhiteCastleable()) {
			if (iterationChessMoveUnit.isKing() && !isBlackIterationChessMoveUnit) {
				iterationChessBoard.setWhiteCastleableKingside(false);
				iterationChessBoard.setWhiteCastleableQueenside(false);
			}
			if (iterationChessMoveUnit.isRook() && (iterationChessMove.getOrigin().toString().equals("a1"))
					&& (!isBlackIterationChessMoveUnit)) {
				iterationChessBoard.setWhiteCastleableQueenside(false);
			}
			if (iterationChessMoveUnit.isRook() && (iterationChessMove.getOrigin().toString().equals("h1"))
					&& (!isBlackIterationChessMoveUnit)) {
				iterationChessBoard.setWhiteCastleableKingside(false);
			}
		}
	}

	private void setCasteableBlack(ChessBoard iterationChessBoard, ChessMove iterationChessMove,
			ChessPiece iterationChessMoveUnit, boolean isBlackIterationChessMoveUnit) {
		if (isBlackCastleable()) {
			if (iterationChessMoveUnit.isKing() && isBlackIterationChessMoveUnit) {
				iterationChessBoard.setBlackCastleableKingside(false);
				iterationChessBoard.setBlackCastleableQueenside(false);
			}
			if (iterationChessMoveUnit.isRook() && (iterationChessMove.getOrigin().toString().equals("a8"))
					&& isBlackIterationChessMoveUnit) {
				iterationChessBoard.setBlackCastleableQueenside(false);
			}
			if (iterationChessMoveUnit.isRook() && (iterationChessMove.getOrigin().toString().equals("h8"))
					&& isBlackIterationChessMoveUnit) {
				iterationChessBoard.setBlackCastleableKingside(false);
			}
		}
	}

	private ChessMove getCastleChessMove(ChessBoard iterationChessBoard, String moveOriginSquareCoordinates,
			String moveDestinationSquareCoordinates, ChessMove chessMove) throws IllegalMoveException {
		ChessPiece e1Occupant = iterationChessBoard.getSquare(5, 1).getOccupant();
		boolean ise1WhiteKing = ((e1Occupant != null) && (e1Occupant.isKing()) && (!e1Occupant.isBlack()));
		ChessPiece e8Occupant = iterationChessBoard.getSquare(5, 8).getOccupant();
		boolean ise8BlackKing = ((e8Occupant != null) && (e8Occupant.isKing()) && (e8Occupant.isBlack()));

		if ((moveOriginSquareCoordinates.equals("e1") && moveDestinationSquareCoordinates.equals("g1") && ise1WhiteKing)
				|| (moveOriginSquareCoordinates.equals("e8") && moveDestinationSquareCoordinates.equals("g8")
						&& ise8BlackKing)) {
			chessMove = new ChessMove(iterationChessBoard, ChessMove.CASTLE_KINGSIDE);
		}
		if ((moveOriginSquareCoordinates.equals("e1") && moveDestinationSquareCoordinates.equals("c1") && ise1WhiteKing)
				|| (moveOriginSquareCoordinates.equals("e8") && moveDestinationSquareCoordinates.equals("c8")
						&& ise8BlackKing)) {
			chessMove = new ChessMove(iterationChessBoard, ChessMove.CASTLE_QUEENSIDE);
		}
		return chessMove;
	}

	private void initShashChess() {
		startShashChess();
		setInitialUciOptions();
		String showInitEngineInfos = getShowEngineInfos();
		if ((showInitEngineInfos != null) && (!showInitEngineInfos.isEmpty()) && (showInitEngineInfos.equalsIgnoreCase("yes"))) {
			retrieveShashChessInfo();
		}
	}

	private String doStep(String fen, int step, boolean isBlackMove) {
		long currentAverageTimeMSForMove = strongestAverageTimeSecondsForMove * 1000;
		uci.uciNewGame();
		uci.positionFen(fen);
		UCIResponse<Analysis> response = uci.analysis(currentAverageTimeMSForMove, timeoutMS);
		Analysis analysis = response.getResultOrThrow();
		Move bestMove = analysis.getBestMove();
		if (bestMove == null) {
			UCIResponse<BestMove> bestMoveResponse = uci.bestMove(currentAverageTimeMSForMove);
			BestMove bestMoveOnly = bestMoveResponse.getResultOrThrow();
			uci.setOption(LIVE_BOOK_PROXY_URL, "", timeoutMS).getResultOrThrow();
			uci.setOption(CHESSDB_TABLEBASE, "false", timeoutMS).getResultOrThrow();
			String searchMoves = bestMoveOnly.getCurrent();
			String goCommand = (searchMoves != null && !searchMoves.isEmpty())
					? String.join("", "go movetime %d ", "searchmoves ", searchMoves)
					: "go movetime %d";
			response = uci.command(format(goCommand, strongestAverageTimeSecondsForMove), UCI.analysis::process,
					breakOn("bestmove"), uci.getDefaultTimeout());
			analysis = response.getResultOrThrow();
			Map<Integer, Move> moves = analysis.getAllMoves();
			bestMove = moves.get(1);
			uci.setOption(LIVE_BOOK_PROXY_URL, livebookProxyUrl, timeoutMS).getResultOrThrow();
			uci.setOption(CHESSDB_TABLEBASE, chessDBTablebase, timeoutMS).getResultOrThrow();
		}
		String lan = bestMove.getLan();
		setIterationScore(((Double) (bestMove.getStrength().getScore() * 100)).intValue());
		setIterationDepth(bestMove.getDepth());
		setCurrentPositionType(getPositionType(iterationScore, fen));
		if (step == 2) {
			setMoveCounter(
					(((semiMoveNumber + 2) % 2 != 0) ? (int) Math.floor((double) (semiMoveNumber + 2) / (double) 2)
							: ((semiMoveNumber + 2) / 2)));
			String notationMsg = String.join("",
					(String.join("", Integer.toString(this.moveCounter), isBlackMove ? "...." : ".")),
					bestMove.getLan(), " ", String.join("", Integer.toString(iterationScore), ";"),
					String.join("", Integer.toString(iterationDepth), ";",
							Integer.toString(WinProbabilityByMaterial.getWinProbabilityFromScore(iterationScore, fen)),
							";"),
					getAbbreviatePositionType(getCurrentPositionType()));
			logger.info(notationMsg);
		}
		return lan;
	}

	private String getAbbreviatePositionType(String positionType) {
		return String.join("", "s-", WinProbabilityByMaterial.getAbbreviateRangeDescription(positionType));
	}

	private void setShashinUciOptions(String positionType) {
		RangeDescription[] rangeDescriptions = RangeDescription.values();
		RangeDescription rangeDescription = null;
		for (RangeDescription currentRangeDescription : rangeDescriptions) {
			if (currentRangeDescription.getDescription().equals(positionType)) {
				rangeDescription = currentRangeDescription;
				break;
			}
		}
		if(rangeDescription!=null) {
			switch (rangeDescription) {
			case HIGH_PETROSIAN:
			case MIDDLE_PETROSIAN:
			case LOW_PETROSIAN:
			case CAPABLANCA:
			case LOW_TAL:
			case MIDDLE_TAL:
			case HIGH_TAL:
				uci.setOption(rangeDescription.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case HIGH_MIDDLE_PETROSIAN:
				uci.setOption(RangeDescription.HIGH_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.MIDDLE_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case MIDDLE_LOW_PETROSIAN:
				uci.setOption(RangeDescription.MIDDLE_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.LOW_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case CAOS_PETROSIAN_CAPABLANCA:
				uci.setOption(RangeDescription.LOW_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.CAPABLANCA.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case CAOS_TAL_CAPABLANCA:
				uci.setOption(RangeDescription.CAPABLANCA.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.LOW_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case LOW_MIDDLE_TAL:
				uci.setOption(RangeDescription.LOW_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.MIDDLE_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case MIDDLE_HIGH_TAL:
				uci.setOption(RangeDescription.MIDDLE_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
				uci.setOption(RangeDescription.HIGH_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
				break;
			case CAOS_TAL_CAPABLANCA_PETROSIAN:
				setAllPersonalities();
				break;
			default:
				break;
			}
			
		}
	}

	private void setAllPersonalities() {
		uci.setOption(RangeDescription.HIGH_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.MIDDLE_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.LOW_PETROSIAN.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.CAPABLANCA.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.LOW_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.LOW_MIDDLE_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.MIDDLE_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
		uci.setOption(RangeDescription.HIGH_TAL.getDescription(), "true", timeoutMS).getResultOrThrow();
	}

	private String getPositionType(int score, String fen) {
		int winProbability = WinProbabilityByMaterial.getWinProbabilityFromScore(score, fen);
		int range = WinProbabilityByMaterial.getRange(winProbability);
		return WinProbabilityByMaterial.getRangeDescription(range);
	}

	private void setInitialUciOptions() {
		try {
			uci.setOption("Threads", Integer.toString(threadsNumber), timeoutMS).getResultOrThrow();
			uci.setOption("Hash", Integer.toString(hashSizeMB), timeoutMS).getResultOrThrow();
			uci.setOption("SyzygyPath", syzygyPath, timeoutMS).getResultOrThrow();
			uci.setOption("SyzygyProbeDepth", syzygyProbeDepth, timeoutMS).getResultOrThrow();
			uci.setOption("Full depth threads", fullDepthThreads, timeoutMS).getResultOrThrow();
			uci.setOption("Variety", variety, timeoutMS).getResultOrThrow();
			uci.setOption("Persisted learning", persistedLearning, timeoutMS).getResultOrThrow();
			uci.setOption("Read only learning", readOnlyLearning, timeoutMS).getResultOrThrow();
			if (livebookProxyUrl != null) {
				uci.setOption(LIVE_BOOK_PROXY_URL, livebookProxyUrl, timeoutMS).getResultOrThrow();
			}
			if (chessDBTablebase != null) {
				uci.setOption(CHESSDB_TABLEBASE, chessDBTablebase, timeoutMS).getResultOrThrow();
			}
			uci.setOption("MCTS", mcts, timeoutMS).getResultOrThrow();
			uci.setOption("MCTSThreads", mCTSThreads, timeoutMS).getResultOrThrow();
		} catch (Exception e) {
			closeWrite();
			closeShashChess();
			logger.info("Impossible to setup uci options");
			System.exit(0);
		}
	}

	@SuppressWarnings("rawtypes")
	private void retrieveShashChessInfo() {
		UCIResponse<EngineInfo> response = uci.getEngineInfo();
		if (response.success()) {

			// Engine name
			EngineInfo engineInfo = response.getResult();
			String engineNameMsg = String.join("", "Engine name:" + engineInfo.getName());
			logger.info(engineNameMsg);

			// Supported engine options
			logger.info("Supported engine options:");
			Map<String, EngineOption> engineOptions = engineInfo.getOptions();
			engineOptions.forEach((key, value) -> {
				logger.info(String.join("", "\t", key));
				logger.info(String.join("", "\t\t", value.toString()));
			});
		}
	}

	private void closeShashChess() {
		uci.close();
	}

	private void writePgn() {
		try {
			pgnWriter.writeGame(getCurrentChessGame());
			pw.print("\r\n");
			if (inputGamesPgn == null) {
				closeWrite();
				closeShashChess();
			}

		} catch (IOException e) {
			logger.info(e.getMessage());
		}
	}

	private void setPgnWriter() throws IOException {
		String appendGamePgn = getAppendGame();
		pw = new PrintWriter(new FileWriter(getPgnOutputFileName(),
				((appendGamePgn != null) && (appendGamePgn.equalsIgnoreCase("Yes")))));
		pgnWriter = new PGNWriter(pw);
	}

	private void closeWrite() {
		pw.close();
		pgnWriter.close();
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

	public String getVariety() {
		return variety;
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

	public void setVariety(String variety) {
		this.variety = variety;
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

	public int getMoveCounter() {
		return moveCounter;
	}

	public void setMoveCounter(int moveCounter) {
		this.moveCounter = moveCounter;
	}

	public ChessGame getCurrentChessGame() {
		return currentChessGame;
	}

	public void setCurrentChessGame(ChessGame currentChessGame) {
		this.currentChessGame = currentChessGame;
	}

	public int getMaxMovesNumber() {
		return maxMovesNumber;
	}

	public void setMaxMovesNumber(int maxMovesNumber) {
		this.maxMovesNumber = maxMovesNumber;
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

	public String getInputGamesPgn() {
		return inputGamesPgn;
	}

	public void setInputGamesPgn(String inputGamesPgn) {
		this.inputGamesPgn = inputGamesPgn;
	}

	public Date getDataInizioElaborazionePrincipale() {
		return dataInizioElaborazionePrincipale;
	}

	public void setDataInizioElaborazionePrincipale(Date dataInizioElaborazionePrincipale) {
		this.dataInizioElaborazionePrincipale = dataInizioElaborazionePrincipale;
	}

	public PGNReader getPgnReader() {
		return pgnReader;
	}

	public void setPgnReader(PGNReader pgnReader) {
		this.pgnReader = pgnReader;
	}

	public FileReader getGamesFileReader() {
		return gamesFileReader;
	}

	public BufferedReader getGamesInputBufferedReader() {
		return gamesInputBufferedReader;
	}

	public void setGamesFileReader(FileReader gamesFileReader) {
		this.gamesFileReader = gamesFileReader;
	}

	public void setGamesInputBufferedReader(BufferedReader gamesInputBufferedReader) {
		this.gamesInputBufferedReader = gamesInputBufferedReader;
	}

	public long getNumeroPartiteTotali() {
		return numeroPartiteTotali;
	}

	public long getNumeroPartiteInserite() {
		return numeroPartiteInserite;
	}

	public long getNumeroPartiteInFormatoScorretto() {
		return numeroPartiteInFormatoScorretto;
	}

	public long getPartitaCorrente() {
		return partitaCorrente;
	}

	public void setNumeroPartiteTotali(long numeroPartiteTotali) {
		this.numeroPartiteTotali = numeroPartiteTotali;
	}

	public void setNumeroPartiteInserite(long numeroPartiteInserite) {
		this.numeroPartiteInserite = numeroPartiteInserite;
	}

	public void setNumeroPartiteInFormatoScorretto(long numeroPartiteInFormatoScorretto) {
		this.numeroPartiteInFormatoScorretto = numeroPartiteInFormatoScorretto;
	}

	public void setPartitaCorrente(long partitaCorrente) {
		this.partitaCorrente = partitaCorrente;
	}

	public ChessGame getCurrentInputChessGame() {
		return currentInputChessGame;
	}

	public void setCurrentInputChessGame(ChessGame currentInputChessGame) {
		this.currentInputChessGame = currentInputChessGame;
	}

	public int getGamesMoveFromEco() {
		return gamesMoveFromEco;
	}

	public void setGamesMoveFromEco(int gamesMoveFromEco) {
		this.gamesMoveFromEco = gamesMoveFromEco;
	}

	public int getSemiMoveNumber() {
		return semiMoveNumber;
	}

	public void setSemiMoveNumber(int semiMoveNumber) {
		this.semiMoveNumber = semiMoveNumber;
	}

	public PGNWriter getPgnWriter() {
		return pgnWriter;
	}

	public PrintWriter getPw() {
		return pw;
	}

	public void setPgnWriter(PGNWriter pgnWriter) {
		this.pgnWriter = pgnWriter;
	}

	public boolean isBlackCastleable() {
		return blackCastleable;
	}

	public void setBlackCastleable(boolean blackCastleable) {
		this.blackCastleable = blackCastleable;
	}

	public boolean isWhiteCastleable() {
		return whiteCastleable;
	}

	public void setWhiteCastleable(boolean whiteCastleable) {
		this.whiteCastleable = whiteCastleable;
	}

	public void setPw(PrintWriter pw) {
		this.pw = pw;
	}

	public String getChessDBTablebase() {
		return chessDBTablebase;
	}

	public void setChessDBTablebase(String chessDBTablebase) {
		this.chessDBTablebase = chessDBTablebase;
	}

	public String getLivebookProxyUrl() {
		return livebookProxyUrl;
	}

	public void setLivebookProxyUrl(String livebookProxyUrl) {
		this.livebookProxyUrl = livebookProxyUrl;
	}

}
