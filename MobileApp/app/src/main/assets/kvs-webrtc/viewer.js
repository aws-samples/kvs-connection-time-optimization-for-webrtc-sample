// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

const viewer = {};
let isVideoPlay = false;
let isSignalingClientOpen = false;
let signalingClientState;
let isReceivedSDPAnswer = false;
let isInSetRemoteDescription = false;
let isSetRemoteDescription = false;
let isStartViewer = false;

let sdpAnswerArray = [];
let candidatesArray = [];
let ttl;
let timeoutId;

function getRandomClientId() {
    return Math.random()
        .toString(36)
        .substring(2)
        .toUpperCase();
}

function getFormValues() {
    return {
        region: $('#region').val(),
        channelName: $('#channelName').val(),
        clientId: getRandomClientId(),
        useTrickleICE: true,
        natTraversalDisabled: false,
        forceTURN: false,
        endpoint: null,
        accessKeyId: $('#accessKeyId').val(),
        secretAccessKey: $('#secretAccessKey').val(),
        sessionToken: null,
    };
}
const formValues = getFormValues();

function onVideoPlay() {
    isVideoPlay = true;
    if (timeoutId) {
        clearTimeout(timeoutId);
    }
    console.log('[viewer] video play');
    console.log('[metrics],Streaming,Streaming,Streaming,end');
}

async function printPeerConnectionStateInfo(event, logPrefix, remoteClientId) {
    const rtcPeerConnection = event.target;
    console.log(logPrefix, 'PeerConnection state:', rtcPeerConnection.connectionState);
    if (rtcPeerConnection.connectionState === 'connected') {
        console.log(logPrefix, 'Connection to peer successful!');
        const stats = await rtcPeerConnection.getStats();
        console.log(logPrefix, 'stats : ' + stats);
        if (!stats) return;

        rtcPeerConnection.getSenders().map(sender => {
            const trackType = sender.track?.kind;
            console.log(logPrefix, 'trackType : ' + trackType);
            if (sender.transport) {
                const iceTransport = sender.transport.iceTransport;
                const logSelectedCandidate = () => {
                    console.log(logPrefix, `Chosen candidate pair (${trackType || 'unknown'}):`, JSON.stringify(iceTransport.getSelectedCandidatePair()));
                    const selectedCandidate = JSON.stringify(iceTransport.getSelectedCandidatePair());
                    console.log(logPrefix, 'selectedCandidate: ' + selectedCandidate);
                    const remoteCandidate = JSON.parse(selectedCandidate).remote.candidate;
                    const remoteObj = remoteCandidate.split(' ');
                    console.log(logPrefix, 'remoteObj: ' + JSON.stringify(remoteObj));
                    if (remoteObj.length >= 12) {
                        const remoteIpProtocol = remoteObj[2];
                        const remoteAddr = remoteObj[4] + ":" + remoteObj[5];
                        const remoteType = remoteObj[7];
                        // Bug fix: Determines whether an array includes a certain element by calling API includes()
                        const remoteRAddr = remoteObj.includes('raddr') ? remoteObj[9] + ":" + remoteObj[11] : '0.0.0.0:0';
                        console.log(logPrefix, '[metrics],Streaming,remote,' + remoteIpProtocol + ',' + remoteAddr + ',' + remoteRAddr + ',' + remoteType);
                    }
                    const localCandidate = JSON.parse(selectedCandidate).local.candidate;
                    const localObj = localCandidate.split(' ');
                    console.log(logPrefix, 'localObj: ' + JSON.stringify(localObj));
                    if (localObj.length >= 12) {
                        const localIpProtocol = localObj[2];
                        const localAddr = localObj[4] + ":" + localObj[5];
                        const localType = localObj[7];
                        const localRAddr = localObj.includes("raddr") ? localObj[9] + ":" + localObj[11] : '0.0.0.0:0';
                        console.log(logPrefix, '[metrics],Streaming,local,' + localIpProtocol + ',' + localAddr + ',' + localRAddr + ',' + localType);
                    }
                };
                iceTransport.onselectedcandidatepairchange = logSelectedCandidate;
                logSelectedCandidate();
            } else {
                console.error('Failed to fetch the candidate pair!');
            }
        });
    } else if (rtcPeerConnection.connectionState === 'failed') {
        if (!isVideoPlay) {
            console.error(logPrefix, `[toast] : Connection to ${remoteClientId || 'peer'} failed : 2`);
            stop();
        }
    }
}

async function replaceUriWithIp(uris) {
    // uris type is object
    for (const v in uris) {
        const oldItem = uris[v].split(':');
        const addr = oldItem[1];
        const ip = addr.split(".")[0].replaceAll('-', '.');
        oldItem.splice(1, 1, ip);
        const newItem = oldItem.join(":");
        uris.splice(v, 1 , newItem);
    }
    console.log('[viewer] uris = ', uris);
}

async function onSdpAnswerChanged(sdpAnswerArray) {
    if (sdpAnswerArray.length > 0 && !isInSetRemoteDescription) {
        await setRemoteDescription(sdpAnswerArray[0]);
    } else {
        console.log('[viewer] sdp answer length = ' + sdpAnswerArray.length);
    }
}

async function start(startView) {
    console.log("[viewer] channel = ", formValues.channelName);

    isStartViewer = startView;
    console.log("[viewer] isStartViewer = ", isStartViewer);

    viewer.remoteView = document.getElementById("remoteVideo");

    // Assign an onplay update event to the video element, and execute a function if the current playback position has changed
    viewer.remoteView.onplay = function() { onVideoPlay() };

    // Create KVS client
    const kinesisVideoClient = new AWS.KinesisVideo({
        region: formValues.region,
        accessKeyId: formValues.accessKeyId,
        secretAccessKey: formValues.secretAccessKey,
        sessionToken: formValues.sessionToken,
        endpoint: formValues.endpoint,
        correctClockSkew: true,
    });
    console.log('[viewer] new kinesis video client');

    // get signaling channel ARN
    if (!viewer.channelARN) {
        console.log('[metrics],API,HTTPS,describeSignalingChannel,start');
        const describeSignalingChannelResponse = await kinesisVideoClient
            .describeSignalingChannel({
                ChannelName: formValues.channelName,
            })
            .promise();
        console.log('[metrics],API,HTTPS,describeSignalingChannel,end');
        viewer.channelARN = describeSignalingChannelResponse.ChannelInfo.ChannelARN;
        console.log('[VIEWER] Channel ARN:', viewer.channelARN);
    }

    // get signaling channel endpoints
    if (!viewer.endpointsByProtocol) {
        console.log('[metrics],API,HTTPS,getSignalingChannelEndpoint,start');
        const getSignalingChannelEndpointResponse = await kinesisVideoClient
            .getSignalingChannelEndpoint({
                ChannelARN: viewer.channelARN,
                SingleMasterChannelEndpointConfiguration: {
                    Protocols: ['WSS', 'HTTPS'],
                    Role: KVSWebRTC.Role.VIEWER,
                },
            })
            .promise();
        console.log('[metrics],API,HTTPS,getSignalingChannelEndpoint,end');
        viewer.endpointsByProtocol = getSignalingChannelEndpointResponse.ResourceEndpointList.reduce((endpoints, endpoint) => {
            endpoints[endpoint.Protocol] = endpoint.ResourceEndpoint;
            return endpoints;
        }, {});
        console.log("[viewer] Endpoints: ", JSON.stringify(viewer.endpointsByProtocol));
    }

    // get ice server configuration
    if (!viewer.iceServers) {
        await updateIceServers(false);
        setInterval(function () {
                updateIceServers(true);
            }, (ttl - 30) * 1000);
    }

    console.log('[viewer] ICE servers ttl : ', ttl);
    console.log('[viewer] ICE servers : ', JSON.stringify(viewer.iceServers));

    // create signaling client
    if (!viewer.signalingClient) {
        let channelARN = viewer.channelARN;
        let endpointsByProtocol = viewer.endpointsByProtocol;
        viewer.signalingClient = new KVSWebRTC.SignalingClient({
            channelARN,
            channelEndpoint: endpointsByProtocol.WSS,
            clientId: formValues.clientId,
            role: KVSWebRTC.Role.VIEWER,
            region: formValues.region,
            credentials: {
                accessKeyId: formValues.accessKeyId,
                secretAccessKey: formValues.secretAccessKey,
                sessionToken: formValues.sessionToken,
            },
            systemClockOffset: kinesisVideoClient.config.systemClockOffset,
        });
        console.log('[viewer] Signaling Client : ', JSON.stringify(viewer.signalingClient));

        viewer.signalingClient.on('open', async () => {
            console.log('[metrics],API,WSS,ConnectAsViewer,end');
            if (!isStartViewer) {
                console.log('[metrics],PreConnection,PreConnection,PreConnection,end');
            }
            isSignalingClientOpen = true;
            signalingClientState = 'open';
            console.log('isStartViewer = ', isStartViewer);
            if (isStartViewer) {
                await startPeerConnect();
            }
        });

        // when the SDP answer is received back from the master, add it to the peer connection.
        viewer.signalingClient.on('sdpAnswer', async answer => {
            // add the SDP answer to the peer connection
            isReceivedSDPAnswer = true;
            console.log('[viewer] onSdpAnswer, answer === ' + JSON.stringify(answer));
            console.log('[viewer] onSdpAnswer, answer type === ' + answer.type);
            if (timeoutId) {
                clearTimeout(timeoutId);
            }
            if (!isSetRemoteDescription) {
                sdpAnswerArray.push(answer);
                await onSdpAnswerChanged(sdpAnswerArray);
            } else {
                console.log('[viewer] set remote description finished');
            }
        });

        // when an ICE candidate is received from the master, add it to the peer connection.
        viewer.signalingClient.on('iceCandidate', candidate => {
            // add the ICE candidate received from the MASTER to the peer connection
            console.log('[viewer] onIceCandidate, remote candidate = ' + JSON.stringify(candidate));
            candidatesArray.push(candidate);
            if (isSetRemoteDescription) {
                addIceCandidate();
            }
        });

        viewer.signalingClient.on('close', () => {
            signalingClientState = 'close';
            isReceivedSDPAnswer = false;
            isSetRemoteDescription = false;
            console.log('[viewer] Disconnected from signaling channel');
            isSignalingClientOpen = false;
            isStartViewer = false;
        });

        viewer.signalingClient.on('error', error => {
            if (signalingClientState !== 'error') {
                console.error(`[toast] : Signaling client failed ${error} : 0`);
            }
            isSignalingClientOpen = false;
            signalingClientState = 'error';
        });
    } else {
        console.log('[viewer] signalingClient has existed');
    }

    if (!isSignalingClientOpen) {
        console.log('[metrics],API,WSS,ConnectAsViewer,start');
        viewer.signalingClient.open();
    } else {
        await startPeerConnect();
    }
}

async function updateIceServers(isRefresh) {
    console.log('updateIceServers, isRefresh = ' + isRefresh)
    if (!viewer.kinesisVideoSignalingChannelsClient) {
        // create KVS signaling Client
        const kinesisVideoSignalingChannelsClient = new AWS.KinesisVideoSignalingChannels({
            region: formValues.region,
            accessKeyId: formValues.accessKeyId,
            secretAccessKey: formValues.secretAccessKey,
            sessionToken: formValues.sessionToken,
            endpoint: viewer.endpointsByProtocol.HTTPS,
            correctClockSkew: true,
        });
        console.log('[viewer] new kinesis video signaling channels client');
        viewer.kinesisVideoSignalingChannelsClient = kinesisVideoSignalingChannelsClient;
    }

    // note: collect metrics only when get ice server config at first time
    if (!isRefresh) {
        console.log('[metrics],API,HTTPS,getIceServerConfig,start');
    }
    const getIceServerConfigResponse = await viewer.kinesisVideoSignalingChannelsClient
        .getIceServerConfig({
            ChannelARN: viewer.channelARN,
        })
        .promise();
    // note: collect metrics only when get ice server config at first time
    if (!isRefresh) {
        console.log('[metrics],API,HTTPS,getIceServerConfig,end');
    }

    viewer.iceServers = [];
    if (!formValues.natTraversalDisabled && !formValues.forceTURN) {
        if(formValues.region.indexOf('cn-') >= 0) {
            viewer.iceServers.push({ urls: `stun:stun.kinesisvideo.${formValues.region}.amazonaws.com.cn:443` });
        } else {
            viewer.iceServers.push({ urls: `stun:stun.kinesisvideo.${formValues.region}.amazonaws.com:443` });
        }
    }
    if (!formValues.natTraversalDisabled) {
        getIceServerConfigResponse.IceServerList.forEach(iceServer => {
                replaceUriWithIp(iceServer.Uris);
                viewer.iceServers.push({
                    urls: iceServer.Uris,
                    username: iceServer.Username,
                    credential: iceServer.Password,
                });
                // default: 300 second
                ttl = iceServer.Ttl;
            }
        );
    }
}

async function startPeerConnect() {
    console.log('[viewer] start peer connection');
    // start a new RTCPeerConnection
    let iceServers = viewer.iceServers;
    const configuration = {
        iceServers,
        iceTransportPolicy: formValues.forceTURN ? 'relay' : 'all',
    };
    viewer.peerConnection = new RTCPeerConnection(configuration);

    // send any ice candidates generated by the peer connection to the other peer
    viewer.peerConnection.addEventListener('icecandidate', ({candidate}) => {
        if (candidate) {
            // when trickle ICE is enabled, send the ICE candidates as they are generated.
            if (formValues.useTrickleICE) {
                console.log("[viewer] SendIceCandidate, local candidate = " + JSON.stringify(candidate));
                viewer.signalingClient.sendIceCandidate(candidate);
            }
        } else {
            console.log('[viewer] All ICE candidates have been generated');

            // when trickle ICE is disabled, send the offer now that all the ICE candidates have been generated.
            if (!formValues.useTrickleICE) {
                console.log('[viewer] Sending SDP offer');
                viewer.signalingClient.sendSdpOffer(viewer.peerConnection.localDescription);
            }
        }
    });

    // as remote tracks are received, add them to the remote view
    viewer.peerConnection.addEventListener('track', event => {
        console.log('[viewer] peerConnection ontrack, remoteView srcObject', JSON.stringify(viewer.remoteView.srcObject));

        viewer.remoteStream = event.streams[0];
        viewer.remoteView.srcObject = viewer.remoteStream;
    });

    viewer.peerConnection.addEventListener('connectionstatechange', async event => {
        printPeerConnectionStateInfo(event, '');
    });

    // create an SDP offer to send to the master
    const offer = await viewer.peerConnection.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: true,
    });
    console.log('[viewer] create SDP offer');
    await viewer.peerConnection.setLocalDescription(offer);

    if (formValues.useTrickleICE) {
        console.log('[viewer] send SDP offer');
        console.log('[metrics],API,WSS,SDP,start');
        viewer.signalingClient.sendSdpOffer(viewer.peerConnection.localDescription);
        // set timeout
        timeoutId = setTimeout(function () {
            console.log(`[toast] : received sdp answer time out : 1`);
            // stop listener after timeout
            stop();
        }, 15000);
    }
}

async function setRemoteDescription(answer) {
    console.log('[viewer] setRemoteDescription');
    isInSetRemoteDescription = true;
    await viewer.peerConnection.setRemoteDescription(answer,
        () => {
            isInSetRemoteDescription = false;
            isSetRemoteDescription = true;
            console.log('[metrics],API,WSS,SDP,end');
            console.log('[viewer] setRemoteDescription successCallback');
            addIceCandidate();
        },
        (error) => {
            isInSetRemoteDescription = false;
            onSdpAnswerChanged(sdpAnswerArray);
            console.log('[viewer] setRemoteDescription RTCPeerConnectionErrorCallback : ', error.message);
        });
    sdpAnswerArray.splice(0, 1);
}

function addIceCandidate() {
    console.log('addIceCandidate, candidates size = ', candidatesArray.length);
    for (const index in candidatesArray) {
        const candidate = candidatesArray[index];
        viewer.peerConnection.addIceCandidate(candidate, () => {
            console.log('[viewer] addIceCandidate successCallback');
        }, (error) => {
            console.log('[viewer] addIceCandidate RTCPeerConnectionErrorCallback : ', error);
        });
        candidatesArray.splice(index, 1);
    }
}

function stop() {
    console.log('[viewer] stopping viewer connection');
    isVideoPlay = false;
    isReceivedSDPAnswer = false;
    isSetRemoteDescription = false;
    isStartViewer = false;
    sdpAnswerArray = [];
    candidatesArray = [];
    if (viewer.peerConnection) {
        viewer.peerConnection.close();
        viewer.peerConnection = null;
    }

    if (viewer.remoteStream) {
        viewer.remoteStream.getTracks().forEach(track => track.stop());
        viewer.remoteStream = null;
    }

    if (viewer.remoteView) {
        viewer.remoteView.srcObject = null;
    }
}
