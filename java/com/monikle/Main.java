package com.monikle;

import com.monikle.memdb.MovieDatabase;
import com.monikle.neuro.FeedForwardNetwork;
import com.monikle.neuro.NeuralNetwork;
import com.monikle.neuro.TrainerConfiguration;
import com.monikle.neuro.TrainingData;
import com.monikle.webserver.Config;
import com.monikle.webserver.models.MovieDetail;
import com.monikle.webserver.tmdb.MovieAPI;
import com.monikle.webserver.transformers.JsonTransformer;
import com.monikle.webserver.viewmodels.MovieViewModel;
import spark.Route;

import java.util.List;

import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.get;

public class Main {
	public static NeuralNetwork net;

	public static void main(String[] args) {
		//net = new FeedForwardNetwork(Config.NODE_INDICES.size(), Config.NODE_INDICES.size() * 4, 5, 0.1, 0.9);

		initSparkServer();
	}

	private static void initSparkServer() {
		externalStaticFileLocation("./public"); // Static files

		String DEBUG_USERNAME = "test";

		jsonGet("/movies/popular/:page", (req, res) ->  {
			List<MovieDetail> popular = MovieAPI.popular(Integer.parseInt(req.params("page")));
			return popular.parallelStream().map(movie -> new MovieViewModel(DEBUG_USERNAME, movie)).toArray();
		});

		get("/users/rate/movie/:id", (req, res) -> {
			MovieDatabase db = MovieDatabase.getDb();

			int movieId = Integer.parseInt(req.params("id"));
			int rating = Integer.parseInt(req.queryParams("rating"));

			db.ratings.save(DEBUG_USERNAME, movieId, rating); // Update rating

			long modCount = db.ratings.modificationCount(DEBUG_USERNAME);
			if (modCount > 0 && modCount % Config.UPDATE_NET_MODIFICATION_COUNT == 0) {
				TrainingData data = new TrainingData();

//				TrainerConfiguration trainingConfig = TrainerConfiguration.create()
			}

			return "ok";
		});
	}

	private static void jsonGet(String path, Route route) {
		get(path, "application/json", route, new JsonTransformer());
	}
}
