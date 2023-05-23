package com.alphachess.shashchessanalyzer;

import static java.lang.String.format;
import static java.util.function.Function.identity;
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

import com.alphachess.shashchessanalyzer.WinProbabilityByShashin.Range;
import com.alphachess.shashchessanalyzer.WinProbabilityByShashin.RangeDescription;

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
import ictk.boardgame.chess.ChessPiece;
import ictk.boardgame.chess.ChessPlayer;
import ictk.boardgame.chess.ChessResult;
import ictk.boardgame.chess.Knight;
import ictk.boardgame.chess.Queen;
import ictk.boardgame.chess.Rook;
import ictk.boardgame.chess.Square;
import ictk.boardgame.chess.io.ChessAnnotation;
import ictk.boardgame.chess.io.FEN;
import ictk.boardgame.chess.io.PGNReader;
import ictk.boardgame.chess.io.PGNWriter;
import ictk.boardgame.io.InvalidGameFormatException;
import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.UCIResponse;
import net.andreinc.neatchess.client.model.Analysis;
import net.andreinc.neatchess.client.model.EngineInfo;
import net.andreinc.neatchess.client.model.Move;
import net.andreinc.neatchess.client.model.option.EngineOption;

public class ShashChessPlayer {
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
	private WinProbabilityByShashin winProbabilityByShashin = new WinProbabilityByShashin();
	private boolean blackCastleable=true;
	private boolean whiteCastleable=true;
	
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
		setOpeningVariety(shashChessPlayerProperties.getProperty("openingVariety"));
		setPersistedLearning(shashChessPlayerProperties.getProperty("persistedLearning"));
		setReadOnlyLearning(shashChessPlayerProperties.getProperty("readOnlyLearning"));
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
		return (long) (getHashSizeMB() * 512 / (getThreadsNumber() * getCpuMhz()));
	}

	private Properties getShashChessPlayerProperties(String[] args) {
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
			shashChessPlayer.setPgnWriter();
			shashChessPlayer.initShashChess();
			System.out.println(String.join(" ", "Begin playing at",
					Long.toString(shashChessPlayer.getStrongestAverageTimeSeconds() * 2),
					"seconds per move from position"));
			String iterationFen = shashChessPlayer.getCurrentFen().trim();
			String inputGamesPgn = shashChessPlayer.getInputGamesPgn();
			if (inputGamesPgn != null && !inputGamesPgn.isEmpty()) {
				shashChessPlayer.playFromPgnInput();
			} else {
				shashChessPlayer.playFromIterationFen(iterationFen, false);
			}
		} catch (Exception e) {
			shashChessPlayer.closeWrite();
			shashChessPlayer.closeShashChess();
			System.out.println("End computation for timeout");
			System.exit(0);
		}
	}

	private void playFromPgnInput() throws IllegalMoveException, OutOfTurnException, AmbiguousMoveException {
		setDataInizioElaborazionePrincipale(new Date());
		int currentInputGameNumber = 0;
		try {
			pgnReader = getPGNReader();
			ChessGame currentInputGame = getCurrentInputGame();
			while (currentInputGame != null) {
				System.out.println(currentInputGame.getGameInfo());
				System.out.println(currentInputGame.getHistory());
				System.out.println("");
				setCurrentInputChessGame(currentInputGame);
				currentInputGameNumber++;
				System.out.println("Current input game: " + currentInputGameNumber);
				setCurrentChessGameFromInput(currentInputGame);
				History currentInputHistory = getCurrentInputHistory(currentInputGame);
				setMoveCounter(
						(((semiMoveNumber + 1) % 2 != 0) ? (int) Math.floor((double) (semiMoveNumber + 1) / (double) 2)
								: ((semiMoveNumber + 1) / 2)));
				ChessGameInfo currentInputChessGameInfo = (ChessGameInfo) (currentInputGame.getGameInfo());
				String ecoCode = currentInputChessGameInfo.getECO() != null ? currentInputChessGameInfo.getECO()
						: currentInputGame.getGameInfo().getSite();
				setEcoCode(ecoCode);
				setMaxMovesNumber(getMoveCounter() + getGamesMoveFromEco());
				playFromIterationFen(currentInputHistory, true);
				currentInputGame = getCurrentInputGame();
			}
			closeAll();
		} catch (IllegalMoveException | AmbiguousMoveException | IOException e) {
			// TODO Auto-generated catch block
			logger.info(e.getMessage());
		}

	}

	private void closeAll() throws IOException {
		closeWrite();
		closeShashChess();
		System.out.println("Engine closed");
		System.exit(0);
		closeGamesReader();
	}

	private void playFromIterationFen(History currentInputHistory, boolean fromPgn)
			throws IOException, IllegalMoveException, OutOfTurnException, AmbiguousMoveException {
		ChessMove currentMove = (ChessMove) currentInputHistory.getCurrentMove();
		String iterationFen = getFen().boardToString((ChessBoard) currentMove.getBoard());
		playFromIterationFen(iterationFen, fromPgn);
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

	private void playFromIterationFen(String iterationFen, boolean fromPgn)
			throws IOException, IllegalMoveException, OutOfTurnException, AmbiguousMoveException {
		if (iterationFen != null && !iterationFen.isEmpty()) {
			ChessBoard iterationChessBoard = (ChessBoard) getFen().stringToBoard(iterationFen);
			if (iterationChessBoard != null) {
				if (inputGamesPgn == null) {
					System.out.println("");
					System.out.println(iterationChessBoard.toString());
					System.out.println("");
				}
				System.out.println("Starting self play");
				History currentHistory = getCurrentHistory(iterationChessBoard);
				if (currentHistory != null) {
					while ((!iterationChessBoard.isCheckmate() && (getSemiMoveNumber() < getMaxMovesNumber() * 2))
							&& (!iterationChessBoard.is50MoveRuleApplicible())) {
						doStep(iterationFen, 1, getMoveCounter(), iterationChessBoard.isBlackMove());
						closeShashChess();
						initShashChess();
						setShashinUciOptions(getCurrentPositionType());
						iterationFen = getStep2Fen(iterationFen, iterationChessBoard, currentHistory);
					}

				}
			}
			writeCurrentGame();
		}
	}

	private void writeCurrentGame() throws IOException {
		System.out.println("");
		int finalScore = getIterationScore();
		String currentFen = getCurrentFen().trim();
		ChessResult gameResult = getGameResult(finalScore, currentFen);
		getCurrentChessGameInfo().setResult(gameResult);
		System.out.println(getCurrentChessGame().getGameInfo().toString());
		System.out.println(getCurrentChessGame().getHistory().toString());
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
					System.out.println(String.join("", "Loaded game: ", Long.toString(numeroPartiteTotali)));
				} else {
					return null;
				}

			} catch (InvalidGameFormatException | IllegalMoveException | AmbiguousMoveException ex) {
				numeroPartiteInFormatoScorretto++;
				System.out.println(
						String.join("", "Game ", Long.toString(numeroPartiteTotali + 1), " has an incorrect pgn."));
			} catch (IOException ioEx) {
				return null;
			}
		}
		return currentInputGame;
	}

	private PGNReader getPGNReader() throws FileNotFoundException {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int gamePly = finalChessBoard.getCurrentMoveNumber();
		int winProbability = winProbabilityByShashin.getWinProbability(finalScore, gamePly);
		int rangeValue = winProbabilityByShashin.getRange(winProbability);
		ChessResult gameResult = new ChessResult(Result.UNDECIDED);

		if (rangeValue == Range.SHASHIN_POSITION_CAPABLANCA.getValue()) {
			gameResult.set(ChessResult.DRAW);
		}
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
		return gameResult;
	}

	private History getCurrentHistory(ChessBoard iterationChessBoard) {
		setCurrentChessGame(getCurrentChessGame(iterationChessBoard));
		ChessGameInfo currentChessGameInfo = getChessGameInfo();
		if (getCurrentInputChessGame() == null) {
			getCurrentChessGame().setGameInfo(currentChessGameInfo);
		}
		History currentHistory = getCurrentChessGame().getHistory();
		return currentHistory;
	}

	private ChessGame getCurrentChessGame(ChessBoard iterationChessBoard) {
		ChessGame currentChessGame = (getCurrentInputChessGame() == null) ? new ChessGame() : getCurrentChessGame();
		if (getCurrentInputChessGame() == null) {
			currentChessGame.setBoard(iterationChessBoard);
		}
		return currentChessGame;
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
			throws IllegalMoveException, OutOfTurnException, AmbiguousMoveException {
		String lan2 = doStep(iterationFen, 2, getMoveCounter(), iterationChessBoard.isBlackMove());
		if (lan2 != null) {
			String origSquareStr = lan2.substring(0, 2);
			Square origSquare = iterationChessBoard.getSquare((byte) (origSquareStr.charAt(0) - 96),
					Byte.parseByte(origSquareStr.substring(1)));
			String destSquareStr = lan2.substring(2, 4);
			Square destSquare = iterationChessBoard.getSquare((byte) (destSquareStr.charAt(0) - 96),
					Byte.parseByte(destSquareStr.substring(1)));
			if ((origSquareStr != null) && (origSquare != null) && (destSquareStr != null) && (destSquare != null)) {
				iterationFen = getCurrentIterationFen(iterationChessBoard, currentHistory, lan2, origSquareStr,
						origSquare, destSquareStr, destSquare);
			}
		}
		return iterationFen;
	}

	private String getCurrentIterationFen(ChessBoard iterationChessBoard, History currentHistory, String lan2,
			String origSquareStr, Square origSquare, String destSquareStr, Square destSquare)
			throws IllegalMoveException, OutOfTurnException, AmbiguousMoveException {
		String iterationFen = null;
		ChessMove iterationChessMove = null;
		iterationChessMove = getPromotionMove(iterationChessBoard, lan2, origSquare, destSquare, iterationChessMove);
		iterationChessMove = getCastleMove(iterationChessBoard, origSquareStr, destSquareStr, iterationChessMove);
		if (iterationChessMove == null) {
			iterationChessMove = new ChessMove(iterationChessBoard, origSquare, destSquare);
		}
		if (iterationChessMove != null) {
			currentHistory.add(iterationChessMove);
			semiMoveNumber++;
			setMoveCounter(
					(((semiMoveNumber + 1) % 2 != 0) ? (int) Math.floor((double) (semiMoveNumber + 1) / (double) 2)
							: ((semiMoveNumber + 1) / 2)));
		}
		ChessAnnotation iterationChessMoveAnnotation = new ChessAnnotation();
		iterationChessMoveAnnotation.setComment(String.join("", Integer.toString(getIterationScore()), ";",
				Integer.toString(getIterationDepth()), ";",
				Integer.toString(winProbabilityByShashin.getWinProbability(getIterationScore(),
						iterationChessBoard.getCurrentMoveNumber())),
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
		if(isWhiteCastleable()) {
			if (iterationChessMoveUnit.isKing()&&!isBlackIterationChessMoveUnit)
			{
				iterationChessBoard.setWhiteCastleableKingside(false);
				iterationChessBoard.setWhiteCastleableQueenside(false);
			}
			if(iterationChessMoveUnit.isRook()&&(iterationChessMove.getOrigin().toString().equals("a1"))&&(!isBlackIterationChessMoveUnit))
			{
				iterationChessBoard.setWhiteCastleableQueenside(false);
			}
			if(iterationChessMoveUnit.isRook()&&(iterationChessMove.getOrigin().toString().equals("h1"))&&(!isBlackIterationChessMoveUnit))
			{
				iterationChessBoard.setWhiteCastleableKingside(false);
			}
		}
	}

	private void setCasteableBlack(ChessBoard iterationChessBoard, ChessMove iterationChessMove,
			ChessPiece iterationChessMoveUnit, boolean isBlackIterationChessMoveUnit) {
		if(isBlackCastleable()) {
			if (iterationChessMoveUnit.isKing()&&isBlackIterationChessMoveUnit)
			{
				iterationChessBoard.setBlackCastleableKingside(false);
				iterationChessBoard.setBlackCastleableQueenside(false);
			}
			if(iterationChessMoveUnit.isRook()&&(iterationChessMove.getOrigin().toString().equals("a8"))&&isBlackIterationChessMoveUnit)
			{
				iterationChessBoard.setBlackCastleableQueenside(false);
			}
			if(iterationChessMoveUnit.isRook()&&(iterationChessMove.getOrigin().toString().equals("h8"))&&isBlackIterationChessMoveUnit)
			{
				iterationChessBoard.setBlackCastleableKingside(false);
			}
		}
	}

	private ChessMove getCastleMove(ChessBoard iterationChessBoard, String origSquareStr, String destSquareStr,
			ChessMove iterationChessMove) throws IllegalMoveException {
		if (iterationChessMove == null) {
			iterationChessMove = getCastleMove(origSquareStr, destSquareStr, iterationChessMove, iterationChessBoard);
		}
		return iterationChessMove;
	}

	private ChessMove getPromotionMove(ChessBoard iterationChessBoard, String lan2, Square origSquare,
			Square destSquare, ChessMove iterationChessMove) throws IllegalMoveException {
		if (lan2.length() == 5) {
			char promotionUnit = lan2.substring(4, 5).charAt(0);
			iterationChessMove = getPromotionMove(origSquare, destSquare, iterationChessBoard, promotionUnit);
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
		ChessPiece whiteOccupant = iterationChessBoard.getSquare(5, 1).getOccupant();
		boolean isKingWhite = whiteOccupant != null ? whiteOccupant.isKing() : false;
		ChessPiece blackOccupant = iterationChessBoard.getSquare(5, 8).getOccupant();
		boolean isKingBlack = blackOccupant != null ? blackOccupant.isKing() : false;
		if ((isKingWhite && ((origSquareStr.equals("e1") && destSquareStr.equals("g1")))
				|| (isKingBlack && origSquareStr.equals("e8") && destSquareStr.equals("g8")))) {
			currentChessMove = new ChessMove(iterationChessBoard, ChessMove.CASTLE_KINGSIDE);
		}
		if (isKingWhite && ((origSquareStr.equals("e1") && destSquareStr.equals("c1"))
				|| (isKingBlack && origSquareStr.equals("e8") && destSquareStr.equals("c8")))) {
			currentChessMove = new ChessMove(iterationChessBoard, ChessMove.CASTLE_QUEENSIDE);
		}
		return currentChessMove;
	}

	private void initShashChess() {
		startShashChess();
		setInitialUciOptions();
		String showEngineInfos = getShowEngineInfos();
		if ((showEngineInfos != null) && (!showEngineInfos.isEmpty()) && (showEngineInfos.equalsIgnoreCase("yes"))) {
			retrieveShashChessInfo();
		}
	}

	private String doStep(String fen, int step, int moveCounter, boolean isBlackMove) {
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
		setCurrentPositionType(getPositionType(iterationScore, semiMoveNumber));
		if (step == 2) {
			setMoveCounter(
					(((semiMoveNumber + 2) % 2 != 0) ? (int) Math.floor((double) (semiMoveNumber + 2) / (double) 2)
							: ((semiMoveNumber + 2) / 2)));
			System.out.println(String.join("",
					(String.join("", Integer.toString(this.moveCounter), isBlackMove ? "...." : ".")),
					bestMove.getLan()," ",String.join("", Integer.toString(iterationScore), ";"),
					String.join("", Integer.toString(iterationDepth), ";",
							Integer.toString(winProbabilityByShashin.getWinProbability(iterationScore, semiMoveNumber)),
							";"),
					getAbbreviatePositionType(getCurrentPositionType())));
		}
		return lan;
	}
	private String getAbbreviatePositionType(String positionType) {
		return 	String.join("","s-",WinProbabilityByShashin.getAbbreviateRangeDescription(positionType) );
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

	private String getPositionType(int score, int ply) {
		WinProbabilityByShashin winProbabilityByShashin = new WinProbabilityByShashin();
		int winProbability = winProbabilityByShashin.getWinProbability(score, ply);
		int range = winProbabilityByShashin.getRange(winProbability);
		return winProbabilityByShashin.getRangeDescription(range);
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
			closeWrite();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setPgnWriter() throws IOException {
		String appendGame = getAppendGame();
		pw = new PrintWriter(new FileWriter(getPgnOutputFileName(),
				((appendGame != null) && (appendGame.equalsIgnoreCase("Yes"))) ? true : false));
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


}
