package com.google.vr.sdk.rubicon.panopano;

public class Constants {

        /*
         * You should replace these values with your own. See the README for details
         * on what to fill in.
         */
        public static final String COGNITO_POOL_ID = "us-east-2:3a1cc2e2-cb0e-4846-b0d1-669375409936"; //us-east-2:9b4e7f66-0763-40ec-a350-9bc64ccd70da

        /*
         * Region of your Cognito identity pool ID.
         */
        public static final String COGNITO_POOL_REGION = "us-east-2";

        /*
         * Note, you must first create a bucket using the S3 console before running
         * the sample (https://console.aws.amazon.com/s3/). After creating a bucket,
         * put it's name in the field below.
         */
        public static final String BUCKET_NAME = "exploredc";

        /*
         * Region of your bucket.
         */
        public static final String BUCKET_REGION = "us-east-2";

}
