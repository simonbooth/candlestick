const AWS = require("aws-sdk")
const simpleParser = require('mailparser').simpleParser
const htmlToText = require('html-to-text')

var docClient = new AWS.DynamoDB.DocumentClient();

var decodeQuotedPrintable = function(input) {
    return input
        // https://tools.ietf.org/html/rfc2045#section-6.7, rule 3:
        // “Therefore, when decoding a `Quoted-Printable` body, any trailing white
        // space on a line must be deleted, as it will necessarily have been added
        // by intermediate transport agents.”
        .replace(/[\t\x20]$/gm, '')
        // Remove hard line breaks preceded by `=`. Proper `Quoted-Printable`-
        // encoded data only contains CRLF line  endings, but for compatibility
        // reasons we support separate CR and LF too.
        .replace(/=(?:\r\n?|\n|$)/g, '')
        // Decode escape sequences of the form `=XX` where `XX` is any
        // combination of two hexidecimal digits. For optimal compatibility,
        // lowercase hexadecimal digits are supported as well. See
        // https://tools.ietf.org/html/rfc2045#section-6.7, note 1.
        .replace(/=([a-fA-F0-9]{2})/g, function($0, $1) {
            var codePoint = parseInt($1, 16);
            return String.fromCharCode(codePoint);
        });
};
var deviceTable = "candlestick-devices";
exports.handler = async(event, context) => {
    if (event.Records.length == 1) {
        let record = event.Records[0]
        let msg = JSON.parse(record.Sns.Message)
        console.log("content", msg)
        let parsed = await simpleParser(msg.content)
        let content = htmlToText.fromString(parsed.html, {
            wordwrap: 130
          });
        console.log(content)
        // let content = decodeQuotedPrintable(msg.content)
        let url = content ? content.match(/(https:\/\/[^.]*\.?(?:zoom.us|zoomgov.com|zoom.com|zoom.com.cn)\/[^"\s]*)/) : ""
        console.log("url",url)
        let recipients = msg.mail.commonHeaders.to.length
        for (var r = 0; r < recipients; r++) {
            let to = msg.mail.commonHeaders.to[r]
            let toParts = to.match(/([^"<^]+)\+([^@]+)@/)
            if (toParts && toParts.length == 3) {
                console.log('destination', toParts)
                let devices = []
                await docClient.query({
                        TableName: deviceTable,
                        IndexName: 'SubscriberId-index',
                        KeyConditionExpression: 'SubscriberId = :subscriberid',
                        ExpressionAttributeValues: {
                            ':subscriberid': toParts[1] + "+" + toParts[2]
                        }
                    }).promise()
                    .then(function(data) {
                        console.log("found:", JSON.stringify(data, null, 2));
                        if (data.Items) {
                            data.Items.forEach((item) => {
                                if (item.SnsEndpointArn) {
                                    devices.push(item.SnsEndpointArn)
                                }
                            })
                        }
                    })
                    .catch(function(err) {
                        console.error("Unable to query item. Error JSON:", JSON.stringify(err, null, 2), err);
                    });
                if (devices.length > 0) {
                    let customData = {
                        sender: msg.mail.commonHeaders.from[0],
                        subject: msg.mail.commonHeaders.subject
                    }
                    
                    //get meta from candlestick-subscribers
                    let meta= {}
                                    await docClient.query({
                        TableName: 'candlestick-subscribers',
                        KeyConditionExpression: 'SubscriberId = :subscriberid',
                        ExpressionAttributeValues: {
                            ':subscriberid': toParts[1] + "+" + toParts[2]
                        }
                    }).promise()
                    .then(function(data) {
                        console.log("found:", JSON.stringify(data, null, 2));
                        if (data.Items) {
                            data.Items.forEach((item) => {
                                meta=item
                            })
                        }
                    })
                    .catch(function(err) {
                        console.error("Unable to query item. Error JSON:", JSON.stringify(err, null, 2), err);
                    });

                    if(meta){
                        customData.profile= meta.Profile
                    }
                    if (url && url.length > 0) {
                        console.log(url)
                        customData.url = url[1]
                    }
                    
                    for (var i = 0; i < devices.length; i++) {

                        let snsMsg = JSON.stringify({ "GCM": JSON.stringify({ data: customData }) })
                        let snsParameters = {
                            TargetArn: devices[i],
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
                        console.info("EVENT\n" + JSON.stringify(snsParameters, null, 2))
                    }
                }
            }
        }

    }
    else {
        callback("Unexpected record length");
    }
    // const response = {
    //     statusCode: 200,
    //     body: JSON.stringify('Hello from Lambda!'),
    // };
    // return response;
    return context.logStreamName

};
