package com.monikle.webserver.rater;

import com.monikle.memdb.Database;
import com.monikle.models.MovieDetail;
import com.monikle.models.MovieRating;
import com.monikle.neuro.*;
import com.monikle.webserver.Config;

import java.util.List;
import java.util.Map;

/**
 * Mainly just a wrapper around the neural network to handle taking in
 * movie types and converting them to a usable format for the neural network. Then
 * outputing a rating.
 *
 * Author:    Chris Kellendonk
 * Student #: 4810800
 */
public final class MovieRater {
	private static Database db = Database.getDb();

	private final String username;
	private final Map<String, Integer> nodeIndices;
	private final int inputCount, hiddenCount, outputCount;
	private final int minYear, maxYear;
	private final int minVote, maxVote;
	private final double learningRate, momentum;
	private final double acceptableError;
	private final int maxEpochs, maxRetries;

	private volatile boolean isTraining; // Is the network in the process of being trained
	private NeuralNetwork network;

	public MovieRater(String username, int maxRating, int hiddenNodeCount,
										double learningRate, double momentum, double acceptableError, int maxEpochs, int maxRetries) {

		this.nodeIndices = Config.NODE_INDICES;
		this.inputCount = Config.NODE_INDICES.size();
		this.hiddenCount = hiddenNodeCount;
		this.outputCount = maxRating;
		this.learningRate = learningRate;
		this.momentum = momentum;
		this.acceptableError = acceptableError;
		this.maxEpochs = maxEpochs;
		this.maxRetries = maxRetries;

		this.minYear = Config.MIN_YEAR;
		this.maxYear = Config.MAX_YEAR;
		this.minVote = Config.MIN_VOTE;
		this.maxVote = Config.MAX_VOTE;

		this.username = username;

		this.network = createNetwork();

		this.isTraining = false;
	}

	private static double scale(double iMin, double iMax, double min, double max, double input) {
		if (max < min) {
			throw new IllegalArgumentException("Max must be less than min.");
		}

		if (input < iMin || input > iMax) {
			throw new IllegalArgumentException("Input must be in range [" + iMin + ", " + iMax + "]. Was: " + input);
		}

		return (((max - min) * (input - iMin)) / (iMax - iMin)) + min;
	}

	private static int getRatingFromResult(double[] networkResult) {
		double max = 0;
		int rating = 0;

		for (int i = 0; i < networkResult.length; i++) {
			if (networkResult[i] >= max) {
				rating = i + 1;
				max = networkResult[i];
			}
		}

		return rating;
	}

	private NeuralNetwork createNetwork() {
		return new FeedForwardNetwork(inputCount, hiddenCount, outputCount, learningRate, momentum);
	}

	/**
	 * Train the rater on any new input information. This runs in it's own thread so it will not block web requests
	 *
	 * @throws Exception
	 */
	public void train() throws Exception {
		if (!isTraining) {
			new Thread(() -> {
				try {
					isTraining = true;

					List<MovieRating> ratings = db.ratings.getMovieRatings(username);

					TrainingData data = new TrainingData();

					ratings.forEach(rating -> {
						data.add(mapMovieToNetworkInput(rating.getMovie()), mapRatingToNetworkOutput(rating.getRating().orElse(0)));
					});

					TrainerConfiguration config = TrainerConfiguration.create(data)
							.setAcceptableError(acceptableError)
							.setMaxEpochs(maxEpochs)
							.setShuffleTrainingData(true);

					TrainingResult bestResult = null;
					NeuralNetwork bestNet = null;

					for (int i = 0; i < maxRetries + 1; i++) {
						NeuralNetwork tempNet = createNetwork();
						TrainingResult result = tempNet.train(config);

						// Have we met the acceptable error
						if (result.getError() <= acceptableError) {
							bestResult = result;
							bestNet = tempNet;
							break;
						}

						// Save the best network that has been found so far
						if (bestResult == null || result.getError() < bestResult.getError()) {
							bestResult = result;
							bestNet = tempNet;
						}
					}

					// Update the current network
					synchronized (MovieRater.this) {
						MovieRater.this.network = bestNet;
					}
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				} finally {
					isTraining = false;
				}
			}).start();
		}
	}

	/**
	 * Get the rating for a particular movie using this rating system.
	 * @param movie The movie to get the rating for.
	 * @return			The rating for the movie.
	 */
	public synchronized int getRating(MovieDetail movie) {
		return getRatingFromResult(network.run(mapMovieToNetworkInput(movie)).getValues());
	}

	private double[] mapRatingToNetworkOutput(int rating) {
		double[] output = new double[outputCount];
		output[rating - 1] = 1;
		return output;
	}

	/**
	 * Convert a movie and all it's metadata into the double valued
	 * input form for the neural network.
	 *
	 * @param movie
	 * @return
	 */
	private double[] mapMovieToNetworkInput(MovieDetail movie) {
		double[] input = new double[inputCount];

		mapGenreNodes(movie.getGenres(), input);
		mapYearNode(movie.getYear(), input);
		mapEnglishNode(movie.isEnglish(), input);
		mapVoteAverageNode(movie.getVoteAverage(), input);

		return input;
	}

	private void mapEnglishNode(boolean isEnglish, double[] writeTo) {
		writeTo[nodeIndices.get("English")] = isEnglish ? 1 : 0;
	}

	private void mapVoteAverageNode(double vote, double[] writeTo) {
		if (vote < minVote) {
			vote = minVote;
		}
		if (vote > maxVote) {
			vote = maxVote;
		}

		writeTo[nodeIndices.get("VoteAverage")] = scale(minVote, maxVote, -1, 1, vote);
	}

	private void mapYearNode(int year, double[] writeTo) {
		if (year < minYear) {
			year = minYear;
		}
		if (year > maxYear) {
			year = maxYear;
		}

		writeTo[nodeIndices.get("Year")] = scale(minYear, maxYear, -1, 1, year);
	}

	private void mapGenreNodes(String[] genres, double[] writeTo) {
		for (String genre : genres) {
			int index = nodeIndices.getOrDefault(genre, -1);
			if (index != -1) {
				writeTo[index] = 1;
			}
		}
	}
}
