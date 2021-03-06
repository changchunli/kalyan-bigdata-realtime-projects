package com.orienit.kalyan.hadoop.training.ngram;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class NGramKeyJob implements Tool {
	// Initializing configuration object
	private Configuration conf;

	@Override
	public Configuration getConf() {
		return conf; // getting the configuration
	}

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf; // setting the configuration
	}

	@Override
	public int run(String[] args) throws Exception {

		// initializing the job configuration
		Job job = new Job(getConf());

		// setting the job name
		job.setJobName("Orien IT NGramKey Job");

		// to call this as a jar
		job.setJarByClass(this.getClass());

		// setting custom mapper class
		job.setMapperClass(NGramKeyMapper.class);

		// setting custom reducer class
		job.setReducerClass(NGramKeyReducer.class);

		// setting mapper output key class: K2
		job.setMapOutputKeyClass(NGramKey.class);

		// setting mapper output value class: V2
		job.setMapOutputValueClass(LongWritable.class);

		// setting reducer output key class: K3
		job.setOutputKeyClass(NGramKey.class);

		// setting reducer output value class: V3
		job.setOutputValueClass(LongWritable.class);

		// setting the input format class ,i.e for K1, V1
		job.setInputFormatClass(TextInputFormat.class);

		// setting the output format class
		job.setOutputFormatClass(TextOutputFormat.class);

		// setting the input file path
		FileInputFormat.addInputPath(job, new Path(args[0]));

		// setting the output folder path
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		Path outputpath = new Path(args[1]);
		// delete the output folder if exists
		outputpath.getFileSystem(conf).delete(outputpath, true);

		// to execute the job and return the status
		return job.waitForCompletion(true) ? 0 : -1;
	}

	public static void main(String[] args) throws Exception {
		// start the job providing arguments and configurations
		Configuration conf = new Configuration();
		conf.set("ngramcount", args[2]);

		int status = ToolRunner.run(conf, new NGramKeyJob(), args);
		System.out.println("My Status: " + status);
	}
}

class NGramKeyMapper extends Mapper<LongWritable, Text, NGramKey, LongWritable> {

	private int ngramcount;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		ngramcount = context.getConfiguration().getInt("ngramcount", 0);
	}

	@Override
	protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
		// 1.read the line
		String line = value.toString();

		// 2.split the line into words
		String[] words = line.split(" ");

		// 3.assign count(1) to two conscutive words
		for (int i = 0; i < words.length; i++) {
			List<String> nwords = new ArrayList<String>();

			if (i + ngramcount <= words.length) {
				for (int j = i; j < i + ngramcount; j++) {
					nwords.add(words[j]);
				}
				NGramKey nGramKey = new NGramKey();
				nGramKey.setWords(nwords);
				context.write(nGramKey, new LongWritable(1));
			}
		}
	}
}

class NGramKeyReducer extends Reducer<NGramKey, LongWritable, NGramKey, LongWritable> {
	@Override
	protected void reduce(NGramKey key, Iterable<LongWritable> values, Context context)
			throws IOException, InterruptedException {

		// 1.sum the list of values
		long sum = 0;
		for (LongWritable value : values) {
			sum = sum + value.get();
		}

		// 2.assign sum to the corresponding word
		context.write(key, new LongWritable(sum));
	}
}
