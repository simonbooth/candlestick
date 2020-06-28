var AWS = require("aws-sdk");
var crypto = require("crypto");
var docClient = new AWS.DynamoDB.DocumentClient();
const snsClient = new AWS.SNS()

var subscriberTable = "candlestick-subscribers";
var deviceTable = "candlestick-devices";

function randomString(length, chars) {
 if (!chars) {
  chars = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
 }

 var charsLength = chars.length;
 if (charsLength > 256) {
  throw new Error('Argument \'chars\' should not have more than 256 characters' +
   ', otherwise unpredictability will be broken');
 }

 var randomBytes = crypto.randomBytes(length);
 var result = new Array(length);

 var cursor = 0;
 for (var i = 0; i < length; i++) {
  cursor += randomBytes[i];
  result[i] = chars[cursor % charsLength];
 }

 return result.join('');
}
async function getUserData(groupId, userId) {
 let meta = {}
 await docClient.query({
   TableName: 'candlestick-subscribers',
   KeyConditionExpression: 'SubscriberId = :subscriberid',
   ExpressionAttributeValues: {
    ':subscriberid': groupId + "+" + userId
   }
  }).promise()
  .then(function(data) {
   console.log("found:", JSON.stringify(data, null, 2));
   if (data.Items) {
    data.Items.forEach((item) => {
     meta = item
    })
   }
  })
  .catch(function(err) {
   console.error("Unable to query item. Error JSON:", JSON.stringify(err, null, 2), err);
  });
 return meta

}
async function getGroupData(groupId) {
 let meta = {}
 await docClient.query({
   TableName: 'candlestick-groups',
   KeyConditionExpression: 'GroupId = :groupid',
   ExpressionAttributeValues: {
    ':groupid': groupId
   }
  }).promise()
  .then(function(data) {
   console.log("found:", JSON.stringify(data, null, 2));
   if (data.Items) {
    data.Items.forEach((item) => {
     meta = item
    })
   }
  })
  .catch(function(err) {
   console.error("Unable to query item. Error JSON:", JSON.stringify(err, null, 2), err);
  });
 return meta

}
const iosPlatformApplicationARN = process.env.iosPlatformApplicationARN ? process.env.iosPlatformApplicationARN : ""
const andPlatformApplicationARN = process.env.andPlatformApplicationARN ? process.env.andPlatformApplicationARN : ""
const createPlatformEndpoint = async(platform, deviceToken) => {
 let applicationArn = '';
 if (platform === 'ios') {
  applicationArn = iosPlatformApplicationARN;
 }
 else if (platform === 'android') {
  applicationArn = andPlatformApplicationARN;
 }
 let snsParams = {
  Token: deviceToken,
  PlatformApplicationArn: applicationArn
 };
 let response = await snsClient.createPlatformEndpoint(snsParams).promise()
 return response.EndpointArn;

};
const sendMessage = async(targetArn, customData) => {
 let snsMsg = JSON.stringify({ "GCM": JSON.stringify({ data: customData }) })
 let snsParameters = {
  TargetArn: targetArn,
  Message: snsMsg,
  MessageStructure: 'json'

 }
 console.log('publishing', snsParameters)
 var publishTextPromise = new AWS.SNS().publish(snsParameters).promise();

 await publishTextPromise
  .then(function(data) {
   console.info("MessageID is " + data.MessageId);
   //callback(null, data);
  })
  .catch(function(err) {
   console.error(err);
   //callback(err);
  });
}
exports.handler = async(event) => {
 let response = {
  statusCode: 501,
  body: JSON.stringify("not implemented"),
 };
 // TODO implement
 console.log(event)
 console.log(event.requestContext.http.method)
 switch (event.requestContext.http.method) {
  case "POST":
   event.body = JSON.parse(event.body)
   let didFind = false
   let newSubscription = false
   let newOrUpdatedItem = null
   let forDeletionDeviceId = null

   if (event.body.PairingCode && event.body.SubscriberId && event.body.SubscriberId.indexOf('+') > -1) {
    let groupId = event.body.SubscriberId.split('+')[0].toLowerCase()
    let userId = event.body.SubscriberId.split('+')[1].toLowerCase()
    // find item by pairing code, then set subscriberid
    await docClient.query({
      TableName: deviceTable,
      IndexName: 'PairingCode-index',
      KeyConditionExpression: 'PairingCode = :pairingcode',
      ExpressionAttributeValues: {
       ':pairingcode': event.body.PairingCode
      }

     }).promise()
     .then(function(data) {
      console.log("found:", JSON.stringify(data, null, 2));
      if (data.Items && data.Items.length == 1) {
       didFind = true
       newSubscription = true
       newOrUpdatedItem = data.Items[0]
       newOrUpdatedItem.SubscriberId = groupId + '+' + userId
       newOrUpdatedItem.GroupId = groupId
       newOrUpdatedItem.UserId = userId
       delete newOrUpdatedItem.PairingCode
      }
     })
     .catch(function(err) {
      console.error("Unable to query item. Error JSON:", JSON.stringify(err, null, 2), err);
     });
   }
   else if (event.body.DeviceId) {
    // check db for device ID and get state
    await docClient.get({
      TableName: deviceTable,
      Key: {
       DeviceId: event.body.DeviceId
      }
     }).promise()
     .then(function(data) {
      console.log("found:", JSON.stringify(data, null, 2));
      if (data.Item) {
       didFind = true
       newOrUpdatedItem = data.Item
       forDeletionDeviceId = newOrUpdatedItem.DeviceId
       newOrUpdatedItem.DeviceId = randomString(128) //reset the deviceId for next time, always rotate on successful auth
      }
      else {
       newOrUpdatedItem = {
        DeviceId: randomString(128)

       } // new device, store the firebaseToken for later
       if (event.body.FirebaseToken) {
        newOrUpdatedItem.FirebaseToken = event.body.FirebaseToken
       }
      }
     })
     .catch(function(err) {
      console.error("Unable to add item. Error JSON:", JSON.stringify(err, null, 2));
     });
   }
   else if (event.body.FirebaseToken) {
    // find item by firebase token, but unassign it from the subscriber - this could happen if the device is reset / sold / etc
    await docClient.query({
      TableName: deviceTable,
      IndexName: 'FirebaseToken-index',
      KeyConditionExpression: 'FirebaseToken = :firebasetoken',
      ExpressionAttributeValues: {
       ':firebasetoken': event.body.FirebaseToken
      }

     }).promise()
     .then(function(data) {
      console.log("found:", JSON.stringify(data, null, 2));
      if (data.Items && data.Items.length == 1) {
       didFind = true
       newOrUpdatedItem = data.Items[0]
       forDeletionDeviceId = newOrUpdatedItem.DeviceId
       newOrUpdatedItem.DeviceId = randomString(128)
       delete newOrUpdatedItem.SubscriberId
      }
      else {
       newOrUpdatedItem = {
        DeviceId: randomString(128),
        FirebaseToken: event.body.FirebaseToken
       } // new device, store the firebaseToken for later
      }
     })
     .catch(function(err) {
      console.error("Unable to query item. Error JSON:", JSON.stringify(err, null, 2), err);
     });
   }
   else {
    //initial state, new device has no unique device ID - so we create a random one
    newOrUpdatedItem = { DeviceId: randomString(128) }
   }

   if (newOrUpdatedItem && event.body.FirebaseToken) {
    newOrUpdatedItem.FirebaseToken = event.body.FirebaseToken
   }
   if (newSubscription) {
    if (newOrUpdatedItem.FirebaseToken) {
     if (newOrUpdatedItem && !newOrUpdatedItem.SnsEndpointArn) {
      newOrUpdatedItem.SnsEndpointArn = await createPlatformEndpoint("android", newOrUpdatedItem.FirebaseToken)
     }
    }
   }
   if (newOrUpdatedItem && !newOrUpdatedItem.SubscriberId) {
    newOrUpdatedItem.PairingCode = randomString(8, "ABCDEFGHJKLMNPQRSTUVWXYZ") //TODO : Send pairingcode via push notification
    if (process.env.webUrlBase){
     newOrUpdatedItem.PairingUrl = process.env.webUrlBase + newOrUpdatedItem.PairingCode
    }
   }

   // if missing in db - create new one using random deviceId (device cannot self select an ID)
   // generate a pairing token 

   if (newOrUpdatedItem.SubscriberId && !newOrUpdatedItem.GroupId) {
    newOrUpdatedItem.GroupId = newOrUpdatedItem.SubscriberId.split('+')[0].toLowerCase()
   }
   if (newOrUpdatedItem.SubscriberId && !newOrUpdatedItem.UserId) {
    newOrUpdatedItem.UserId = newOrUpdatedItem.SubscriberId.split('+')[1].toLowerCase()
   }
   if (newOrUpdatedItem) {
    if (newOrUpdatedItem.SnsEndpointArn) {
     console.log("Sending activation message")
     let message = {
      command: "activated",
      zoomAppKey: process.env.zoomAppKey ? process.env.zoomAppKey : "",
      zoomAppSecret: process.env.zoomAppSecret ? process.env.zoomAppSecret : "",
      activationData: newOrUpdatedItem
     }
     if (newOrUpdatedItem.GroupId && newOrUpdatedItem.UserId) {
      message.userData = await getUserData(newOrUpdatedItem.GroupId, newOrUpdatedItem.UserId)
     }
     if (newOrUpdatedItem.GroupId) {
      let groupData = await getGroupData(newOrUpdatedItem.GroupId)
      if (groupData.UI && message.userData.Language && groupData.UI[message.userData.Language])
       message.groupUIData = groupData.UI[message.userData.Language]
     }

     await sendMessage(newOrUpdatedItem.SnsEndpointArn, message)
    }
    newOrUpdatedItem.LastUpdated = new Date().toUTCString()
    let createParams = {
     TableName: deviceTable,
     Item: newOrUpdatedItem
    }
    await docClient.put(createParams).promise()
     .then(function(data2) {
      console.log("Added item:", JSON.stringify(data2, null, 2));
     })
     .catch(function(err2) {
      console.error("Unable to add item. Error JSON:", JSON.stringify(err2, null, 2));
     });
   }
   if (forDeletionDeviceId) {
    let deleteParams = {
     TableName: deviceTable,
     Key: { DeviceId: forDeletionDeviceId }
    }
    await docClient.delete(deleteParams).promise()
     .then(function(data3) {
      console.log("Deleted item:", JSON.stringify(data3, null, 2));
     })
     .catch(function(err3) {
      console.error("Unable to delete item. Error JSON:", JSON.stringify(err3, null, 2));
     });
   }

   response = {
    statusCode: 200,
    body: JSON.stringify(newOrUpdatedItem),
   };
   break;
 }



 return response;
};
