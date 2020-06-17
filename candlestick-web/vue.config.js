module.exports = {
  transpileDependencies: ["vuetify"],

  pluginOptions: {
    s3Deploy: {
      registry: undefined,
      awsProfile: "simon-aws-vscode",
      overrideEndpoint: false,
      region: "us-west-2",
      bucket: "nubhub-web",
      createBucket: false,
      staticHosting: false,
      assetPath: "dist",
      assetMatch: "**",
      deployPath: "/",
      acl: "public-read",
      pwa: false,
      enableCloudfront: true,
      cloudfrontId: "EA4UFBTXY7K7V",
      pluginVersion: "4.0.0-rc3",
      uploadConcurrency: 5
    }
  }
};
