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
public class SacrificesFinder {
	private static final int PSYCHOLOGICAL_THRESHOLD = 2;
	private static final int REAL_THRESHOLD = 0;
	UCI uci = null;
	private Properties sacrificesFinderProperties;
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
	private String psychological;
	private static Logger logger = Logger.getLogger(SacrificesFinder.class.getName());

	public SacrificesFinder(String sacrificesFinderPropertiesPath,String fen) {
		sacrificesFinderProperties = getSacrificesFinderProperties(sacrificesFinderPropertiesPath);
		this.fen=fen;
		setInputParameters();
		setTimeoutMS(timeoutSeconds * 1000);
		uci = new UCI(timeoutMS);
	}

	private void setInputParameters() {
		setTimeoutSeconds(Long.parseLong(sacrificesFinderProperties.getProperty("timeoutSeconds")));
		setThreadsNumber(Integer.parseInt(sacrificesFinderProperties.getProperty("threadsNumber")));
		setCpuMhz(Integer.parseInt(sacrificesFinderProperties.getProperty("cpuMhz")));
		setHashSizeMB(Integer.parseInt(sacrificesFinderProperties.getProperty("hashSizeMB")));
		setSyzygyPath(sacrificesFinderProperties.getProperty("syzygyPath"));
		setSyzygyProbeDepth(sacrificesFinderProperties.getProperty("syzygyProbeDepth"));
		setStrongestAverageTimeSecondsForMove(getStrongestAverageTimeSeconds());
		setMultiPV(Integer.parseInt(sacrificesFinderProperties.getProperty("multiPV")));
		setFullDepthThreads(sacrificesFinderProperties.getProperty("fullDepthThreads"));
		setOpeningVariety(sacrificesFinderProperties.getProperty("openingVariety"));
		setPersistedLearning(sacrificesFinderProperties.getProperty("persistedLearning"));
		setReadOnlyLearning(sacrificesFinderProperties.getProperty("readOnlyLearning"));
		setMcts(sacrificesFinderProperties.getProperty("mcts"));
		setMCTSThreads(sacrificesFinderProperties.getProperty("mCTSThreads"));
		setEngineName(sacrificesFinderProperties.getProperty("engineName"));
		setSearchMoves(sacrificesFinderProperties.getProperty("searchMoves"));
		setShowEngineInfos(sacrificesFinderProperties.getProperty("showEngineInfos"));
		setPsychological(sacrificesFinderProperties.getProperty("psychological"));
	}

	public long getStrongestAverageTimeSeconds() {
		return (getHashSizeMB() * 512 / (getThreadsNumber() * getCpuMhz()));
	}

	private Properties getSacrificesFinderProperties(String sacrificesFinderPropertiesPath) {
		Properties properties = new Properties();
		try {
			File file = new File(sacrificesFinderPropertiesPath);
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

		SacrificesFinder sacrificesFinder = new SacrificesFinder(args[0],args[1]);
		try {
			sacrificesFinder.startEngine();
			sacrificesFinder.setInitialUciOptions();
			String showEngineInfos = sacrificesFinder.getShowEngineInfos();
			if ((showEngineInfos != null) && (!showEngineInfos.isEmpty())
					&& (showEngineInfos.equalsIgnoreCase("yes"))) {
				sacrificesFinder.retrieveEngineInfo();
			}
			int threshold=sacrificesFinder.getPsychological().trim().equalsIgnoreCase("yes")?PSYCHOLOGICAL_THRESHOLD:REAL_THRESHOLD;
			List<MoveRangeWinProbability> movesRanges = sacrificesFinder.getSacrifices(sacrificesFinder.getFen(),threshold);
			printSacrifices(movesRanges);
			sacrificesFinder.closeEngine();
		} catch (Exception e) {
			sacrificesFinder.closeEngine();
			logger.info("End computation for timeout");
			System.exit(0);
		}
	}

	private static void printSacrifices(List<MoveRangeWinProbability> movesRanges) {
		if(!movesRanges.isEmpty()) {
			logger.info("Sacrifices:Shashin position type;Win Probability");
			for (MoveRangeWinProbability moveRange : movesRanges) {
				String message=moveRange.getMove() + ": " + WinProbabilityByMaterial.getRangeDescription(moveRange.getRange())+";"+moveRange.getWinProbability();
				logger.info(message);
			}
		}
	}

	private List<MoveRangeWinProbability> getSacrifices(String fenToAnalyze, int threshold) {
		List<MoveRangeWinProbability> movesRanges = new ArrayList<>();
		String message = "";
		try {
			Board currentChessBoard = new Board();
			ChessBoard currentChessBoardICTK = (ChessBoard) new FEN().stringToBoard(fenToAnalyze);
			currentChessBoard.loadFromFen(fenToAnalyze);
			if (!currentChessBoard.isStaleMate() && !currentChessBoard.isMated()) {
				int initialWinProbability=getWinProbability("", fenToAnalyze);
				int initialRange=WinProbabilityByMaterial.getRange(initialWinProbability);
				float sideToMoveCurrentMaterialDifference = getSideToMoveMaterialDifference(currentChessBoardICTK);
				List<com.github.bhlangonijr.chesslib.move.Move> legalMoves = currentChessBoard.legalMoves();
				message = "Analysing " + legalMoves.size() + " moves";
				logger.info(message);
				int moveNumber = 0;
				for (com.github.bhlangonijr.chesslib.move.Move currentLegalMove : legalMoves) {
					moveNumber++;
					message ="Analysing move " + moveNumber;
					logger.info(message);
					String currentLegalMoveSan = currentLegalMove.toString();
					message="Analysing legal move: " + currentLegalMoveSan;
					logger.info(message);
					int currentWinProbability=getWinProbability(currentLegalMoveSan, fenToAnalyze);
					int currentRange=WinProbabilityByMaterial.getRange(currentWinProbability);
					if (isSacrifice(currentLegalMove, currentChessBoard, sideToMoveCurrentMaterialDifference,
							currentChessBoardICTK.isBlackMove())
							&& (Math.abs(currentRange - initialRange) <= threshold)) {
						logger.info("Sacrifice added");
						movesRanges.add(new MoveRangeWinProbability(currentLegalMoveSan,currentRange,currentWinProbability ));
					}
					else {
						logger.info("Not a sacrifice");
					}
				}
				Collections.sort(movesRanges, Comparator.comparingInt(MoveRangeWinProbability::getWinProbability).reversed());
			}
			else {
				message ="StaleMate or Mate: no sacrifices to find";
				logger.info(message);
			}
		} catch (IOException e) {
			logger.info(e.getMessage());
		}
		return movesRanges;
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
			return WinProbabilityByMaterial.getWinProbabilityFromScore(score, currentFen);
		} else {
			return 0;
		}
	}

	public boolean isSacrifice(com.github.bhlangonijr.chesslib.move.Move nextMove, Board currentChessBoard,
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

	public float getSideToMoveMaterialDifference(ChessBoard chessBoard) {
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

	public Properties getSacrificesFinderProperties() {
		return sacrificesFinderProperties;
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


	public String getPsychological() {
		return psychological;
	}

	public void setPsychological(String psychological) {
		this.psychological = psychological;
	}
}
