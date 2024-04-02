package com.alphachess.shashchessanalyzer;

import static java.lang.String.format;
import static net.andreinc.neatchess.client.breaks.Break.breakOn;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;

import ictk.boardgame.chess.ChessBoard;
import ictk.boardgame.chess.ChessPiece;
import ictk.boardgame.chess.Square;
import ictk.boardgame.chess.io.FEN;
import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.UCIResponse;
import net.andreinc.neatchess.client.model.Analysis;
import net.andreinc.neatchess.client.model.EngineInfo;
import net.andreinc.neatchess.client.model.Move;
import net.andreinc.neatchess.client.model.option.EngineOption;

/**
 * Hello world!
 *
 */
public class PsychologicalSacrificesFinder {
	UCI uci = null;
	private Properties psychologicalSacrificesFinderProperties;
	private int threadsNumber;
	private int hashSizeMB;
	private int cpuMhz;
	private String syzygyPath;
	private String syzygyProbeDepth;
	private long strongestAverageTimeSecondsForMove;
	private long timeoutSeconds;
	private long timeoutMS;
	private String fen;
	private int multiPV;
	private String fullDepthThreads;
	private String openingVariety;
	private String persistedLearning;
	private String readOnlyLearning;
	private String mcts;
	private String mCTSThreads;
	private String engineName;
	private String searchMoves;
	private String showEngineInfos;
	private WinProbabilityByShashin winProbabilityByShashin = new WinProbabilityByShashin();
	private static Logger logger = Logger.getLogger(PsychologicalSacrificesFinder.class.getName());

	public PsychologicalSacrificesFinder(String[] args) {
		psychologicalSacrificesFinderProperties = getpsychologicalSacrificesFinderProperties(args);
		setInputParameters();
		setTimeoutMS(timeoutSeconds * 1000);
		uci = new UCI(timeoutMS);
	}

	private void setInputParameters() {
		setTimeoutSeconds(Long.parseLong(psychologicalSacrificesFinderProperties.getProperty("timeoutSeconds")));
		setThreadsNumber(Integer.parseInt(psychologicalSacrificesFinderProperties.getProperty("threadsNumber")));
		setCpuMhz(Integer.parseInt(psychologicalSacrificesFinderProperties.getProperty("cpuMhz")));
		setHashSizeMB(Integer.parseInt(psychologicalSacrificesFinderProperties.getProperty("hashSizeMB")));
		setSyzygyPath(psychologicalSacrificesFinderProperties.getProperty("syzygyPath"));
		setSyzygyProbeDepth(psychologicalSacrificesFinderProperties.getProperty("syzygyProbeDepth"));
		setStrongestAverageTimeSecondsForMove(getStrongestAverageTimeSeconds());
		setFen(psychologicalSacrificesFinderProperties.getProperty("fen"));
		setMultiPV(Integer.parseInt(psychologicalSacrificesFinderProperties.getProperty("multiPV")));
		setFullDepthThreads(psychologicalSacrificesFinderProperties.getProperty("fullDepthThreads"));
		setOpeningVariety(psychologicalSacrificesFinderProperties.getProperty("openingVariety"));
		setPersistedLearning(psychologicalSacrificesFinderProperties.getProperty("persistedLearning"));
		setReadOnlyLearning(psychologicalSacrificesFinderProperties.getProperty("readOnlyLearning"));
		setMcts(psychologicalSacrificesFinderProperties.getProperty("mcts"));
		setMCTSThreads(psychologicalSacrificesFinderProperties.getProperty("mCTSThreads"));
		setEngineName(psychologicalSacrificesFinderProperties.getProperty("engineName"));
		setSearchMoves(psychologicalSacrificesFinderProperties.getProperty("searchMoves"));
		setShowEngineInfos(psychologicalSacrificesFinderProperties.getProperty("showEngineInfos"));
	}

	public long getStrongestAverageTimeSeconds() {
		return (getHashSizeMB() * 512 / (getThreadsNumber() * getCpuMhz()));
	}

	private Properties getpsychologicalSacrificesFinderProperties(String[] args) {
		String psychologicalSacrificesFinderPropertiesName = args[0];
		Properties properties = new Properties();
		try {
			File file = new File(psychologicalSacrificesFinderPropertiesName);
			FileInputStream fileInput = new FileInputStream(file);
			properties.load(fileInput);
			fileInput.close();

		} catch (IOException e) {
			logger.info(e.getMessage());
			System.exit(0);
		}
		return properties;
	}

	public static void main(String[] args) {

		PsychologicalSacrificesFinder psychologicalSacrificesFinder = new PsychologicalSacrificesFinder(args);
		try {
			psychologicalSacrificesFinder.startEngine();
			psychologicalSacrificesFinder.setInitialUciOptions();
			String showEngineInfos = psychologicalSacrificesFinder.getShowEngineInfos();
			if ((showEngineInfos != null) && (!showEngineInfos.isEmpty())
					&& (showEngineInfos.equalsIgnoreCase("yes"))) {
				psychologicalSacrificesFinder.retrieveEngineInfo();
			}
			psychologicalSacrificesFinder.getPsychologicalSacrifices();
			psychologicalSacrificesFinder.closeEngine();
		} catch (Exception e) {
			psychologicalSacrificesFinder.closeEngine();
			logger.info("End computation for timeout");
			System.exit(0);
		}
	}

	private void getPsychologicalSacrifices() {
		try {
			Board currentChessBoard = new Board();
			ChessBoard currentChessBoardICTK = (ChessBoard) new FEN().stringToBoard(fen);
			currentChessBoard.loadFromFen(fen);
			int initialWinProbability=getWinProbability("", fen);
			int initialRange=winProbabilityByShashin.getRange(initialWinProbability);
			if (!currentChessBoard.isStaleMate() && !currentChessBoard.isMated()) {
				float sideToMoveCurrentMaterialDifference = getSideToMoveMaterialDifference(currentChessBoardICTK);
				List<com.github.bhlangonijr.chesslib.move.Move> legalMoves = currentChessBoard.legalMoves();
				List<MoveRange> movesRanges = new ArrayList<>();
				String message = "Analysing " + legalMoves.size() + " moves";
				logger.info(message);
				int moveNumber = 0;
				for (com.github.bhlangonijr.chesslib.move.Move currentLegalMove : legalMoves) {
					moveNumber++;
					message ="Analysing move " + moveNumber;
					logger.info(message);
					String currentLegalMoveSan = currentLegalMove.toString();
					message="Analysing legal move: " + currentLegalMoveSan;
					logger.info(message);
					String currentFen = currentChessBoard.getFen();
					int currentWinProbability=getWinProbability(currentLegalMoveSan, currentFen);
					int currentRange=winProbabilityByShashin.getRange(currentWinProbability);
					if (isSacrifice(currentLegalMove, currentChessBoard, sideToMoveCurrentMaterialDifference,
							currentChessBoardICTK.isBlackMove())
							&& (Math.abs(currentRange - initialRange) <= 2)) {
						logger.info("Psychological Sacrifice added");
						movesRanges.add(new MoveRange(currentLegalMoveSan,currentRange,currentWinProbability ));
					}
					else {
						logger.info("Not a psychological sacrifice");
					}
				}
				Collections.sort(movesRanges, Comparator.comparingInt(MoveRange::getWinProbability).reversed());
				if(!movesRanges.isEmpty()) {
					logger.info("Psychological sacrifices:Shashin position type;Win Probability");
					for (MoveRange moveRange : movesRanges) {
						message=moveRange.getMove() + ": " + winProbabilityByShashin.getRangeDescription(moveRange.getRange())+";"+moveRange.getWinProbability();
						logger.info(message);
					}
				}
			}
		} catch (IOException e) {
			logger.info(e.getMessage());
		}

	}

	private int getWinProbability(String currentLegalMoveSan, String currentFen) {
		long currentAverageTimeMSForMove = strongestAverageTimeSecondsForMove * 1000;
		uci.uciNewGame();
		uci.positionFen(fen);
		String goCommand = currentLegalMoveSan.isEmpty() ? "go movetime %d"
				: String.join("", "go movetime %d ", "searchmoves ", currentLegalMoveSan);
		UCIResponse<Analysis> response = uci.command(format(goCommand, currentAverageTimeMSForMove),
				UCI.analysis::process, breakOn("bestmove"), uci.getDefaultTimeout());
		Analysis analysis = response.getResultOrThrow();
		Move bestMove = analysis.getBestMove();
		if ((bestMove == null) || bestMove.getLan().trim().isEmpty()) {
			searchMoves = uci.bestMove(currentAverageTimeMSForMove).getResultOrThrow().getCurrent();
			goCommand = String.join("", "go movetime %d ", "searchmoves ", searchMoves);
			response = uci.command(format(goCommand, currentAverageTimeMSForMove), UCI.analysis::process,
					breakOn("bestmove"), uci.getDefaultTimeout());
			analysis = response.getResultOrThrow();
			bestMove = analysis.getBestMove();
		}
		if (bestMove != null) {
			int score = ((Double) (bestMove.getStrength().getScore() * 100)).intValue();
			int playedHalfMovesNumber = getPlayedHalfMovesNumber(currentFen);
			return winProbabilityByShashin.getWinProbability(score, playedHalfMovesNumber);
		} else {
			return 0;
		}
	}

	private boolean isSacrifice(com.github.bhlangonijr.chesslib.move.Move nextMove, Board currentChessBoard,
			float sideToMoveMaterialDifference, boolean isBlackInitialSide) {
		long currentAverageTimeMSForMove = strongestAverageTimeSecondsForMove * 1000;
		currentChessBoard.doMove(nextMove);
		String afterNextMoveChessBoardFen = currentChessBoard.getFen();
		Board afterNextMoveBoard = new Board();
		afterNextMoveBoard.loadFromFen(afterNextMoveChessBoardFen);
		currentChessBoard.undoMove();
		uci.uciNewGame();
		uci.positionFen(afterNextMoveChessBoardFen);
		UCIResponse<Analysis> response = uci.analysis(currentAverageTimeMSForMove);
		Analysis analysis = response.getResultOrThrow();
		Move bestMove = analysis.getBestMove();
		String bestMoveLan = bestMove.getLan();
		com.github.bhlangonijr.chesslib.move.Move chessLibMove = null;
		if (bestMoveLan != null) {
			chessLibMove = new com.github.bhlangonijr.chesslib.move.Move(bestMoveLan,
					afterNextMoveBoard.getSideToMove());
		}
		afterNextMoveBoard.doMove(chessLibMove);
		Board afterBestMoveBoard = new Board();
		String afterBestMoveBoardFen = afterNextMoveBoard.getFen();
		afterBestMoveBoard.loadFromFen(afterBestMoveBoardFen);
		afterNextMoveBoard.undoMove();
		if(chessLibMove!=null) {
			boolean isCapture = afterNextMoveBoard.getPiece(chessLibMove.getTo()) != Piece.NONE;
			try {
				if (!isCapture) {
					ChessBoard afterBestMoveChessBoardICTK = (ChessBoard) new FEN().stringToBoard(afterBestMoveBoardFen);
					float difference = (sideToMoveMaterialDifference
							- getSideToMoveMaterialDifference(afterBestMoveChessBoardICTK));
					return (isBlackInitialSide == afterBestMoveChessBoardICTK.isBlackMove()) ? difference > 0
							: difference < 0;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return isSacrifice(chessLibMove, afterNextMoveBoard, sideToMoveMaterialDifference, isBlackInitialSide);
	}

	private float getSideToMoveMaterialDifference(ChessBoard chessBoard) {
		float whiteMaterial = 0;
		float blackMaterial = 0;
		for (int file = 1; file <= 8; file++)
			for (int rank = 1; rank <= 8; rank++) {
				Square currentSquare = chessBoard.getSquare(file, rank);
				ChessPiece currentChessPiece = currentSquare.getOccupant();
				if (currentChessPiece != null) {
					int currentChessPieceIndex = currentChessPiece.getIndex();
					switch (currentChessPieceIndex) {
					case 5: {
						whiteMaterial += 1;
						break;
					}
					case 4: {
						whiteMaterial += 3;
						break;
					}
					case 3: {
						whiteMaterial += 3.5;
						break;
					}
					case 2: {
						whiteMaterial += 5;
						break;
					}
					case 1: {
						whiteMaterial += 9;
						break;
					}
					case 75: {
						blackMaterial += 1;
						break;
					}
					case 74: {
						blackMaterial += 3;
						break;
					}

					case 73: {
						blackMaterial += 3.5;
						break;
					}
					case 72: {
						blackMaterial += 5;
						break;
					}

					case 71: {
						blackMaterial += 9;
						break;
					}
					default: {
						break;
					}
					}
				}
			}
		return chessBoard.isBlackMove() ? (blackMaterial - whiteMaterial) : (whiteMaterial - blackMaterial);
	}

	private int getPlayedHalfMovesNumber(String fen) {
		int playedHalfMovesNumber = -1;
		String[] fenParts = fen.split("\\s+");
		// The half-move number is the fifth element in the FEN string
		String movesNumberStr = fenParts[5];
		String color = fenParts[1];
		try {
			int movesNumber = Integer.parseInt(movesNumberStr);
			playedHalfMovesNumber = color.equals("w") ? (int) ((movesNumber - 1) / 2) : ((movesNumber - 1) / 2) + 1;
		} catch (NumberFormatException e) {
			// Handle the case where the half-move number is not a valid integer
			logger.info("Invalid half-move number in FEN: " + movesNumberStr);
			return playedHalfMovesNumber;
		}
		return playedHalfMovesNumber;
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
			uci.setOption("MultiPV", Integer.toString(multiPV));
		} catch (Exception e) {
			closeEngine();
			logger.info("Impossible to setup uci options");
			System.exit(0);
		}
	}

	@SuppressWarnings("rawtypes")
	private void retrieveEngineInfo() {
		UCIResponse<EngineInfo> response = uci.getEngineInfo();
		if (response.success()) {

			// Engine name
			EngineInfo engineInfo = response.getResult();
			String message = String.join("", "Engine name:" + engineInfo.getName());
			logger.info(message);

			// Supported engine options
			logger.info("Supported engine options:");
			Map<String, EngineOption> engineOptions = engineInfo.getOptions();
			engineOptions.forEach((key, value) -> {
				logger.info(String.join("", "\t", key));
				logger.info(String.join("", "\t\t", value.toString()));
			});
		}
	}

	private void closeEngine() {
		uci.close();
		logger.info("Engine closed");
		System.exit(0);
	}

	private void startEngine() {
		String engineNameWithExtension = String.join("", engineName,
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

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public Properties getpsychologicalSacrificesFinderProperties() {
		return psychologicalSacrificesFinderProperties;
	}

	public long getTimeoutMS() {
		return timeoutMS;
	}

	public void setTimeoutMS(long timeoutMS) {
		this.timeoutMS = timeoutMS;
	}

	public int getMultiPV() {
		return multiPV;
	}

	public void setMultiPV(int multiPV) {
		this.multiPV = multiPV;
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

	public String getFen() {
		return fen;
	}

	public long getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setTimeoutSeconds(long timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public void setFen(String fen) {
		this.fen = fen;
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

	public String getSearchMoves() {
		return searchMoves;
	}

	public void setSearchMoves(String searchMoves) {
		this.searchMoves = searchMoves;
	}

	public String getShowEngineInfos() {
		return showEngineInfos;
	}

	public void setShowEngineInfos(String showEngineInfos) {
		this.showEngineInfos = showEngineInfos;
	}
}
