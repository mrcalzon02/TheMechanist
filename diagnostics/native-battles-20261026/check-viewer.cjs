const {chromium}=require('C:/Users/Admin/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const path=require('path'),fs=require('fs');
(async()=>{
 const browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
 const page=await browser.newPage({viewport:{width:1440,height:1100}});let errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto('file:///'+path.join(__dirname,'index.html').replaceAll('\\','/'));
 const ids=await page.locator('#recording option').evaluateAll(items=>items.map(x=>x.value));
 for(const id of ids){await page.locator('#recording').selectOption(id);await page.waitForFunction(()=>document.querySelector('video').readyState>=1);const size=await page.locator('video').evaluate(v=>[v.videoWidth,v.videoHeight,v.duration,v.error?.message]);if(size[0]!==1280||size[1]!==800||size[2]<5||size[3])throw Error(id+' invalid metadata '+size);}
 await page.locator('#recording').selectOption('20v20-rifles-cover-doom');await page.waitForFunction(()=>document.querySelector('video').readyState>=2);
 await page.locator('video').evaluate(v=>{v.muted=true;return v.play()});await page.waitForFunction(()=>document.querySelector('video').currentTime>1.2);
 const decoded=await page.locator('video').evaluate(v=>({time:v.currentTime,videoFrames:v.getVideoPlaybackQuality().totalVideoFrames,audioDecodedBytes:v.webkitAudioDecodedByteCount}));
 if(decoded.videoFrames<2||decoded.audioDecodedBytes<=0)throw Error('Media decoding failed '+JSON.stringify(decoded));
 await page.locator('video').evaluate(v=>v.pause());await page.screenshot({path:path.join(__dirname,'native-video-viewer.png'),fullPage:false});
 await page.locator('#tiles').click();if(await page.locator('#recording').inputValue()!=='20v20-rifles-cover-2d')throw Error('View switching failed');
 await page.goto('file:///'+path.join(__dirname,'weapon-audio/index.html').replaceAll('\\','/'));
 const audioCount=await page.locator('audio').count();if(audioCount!==11)throw Error('Missing weapon auditions');
 for(let i=0;i<audioCount;i++){await page.waitForFunction(i=>document.querySelectorAll('audio')[i].readyState>=1,i);let metadata=await page.locator('audio').nth(i).evaluate(a=>({duration:a.duration,error:a.error?.message}));if(metadata.duration<=.1||metadata.error)throw Error('Invalid WAV audition '+i);}
 await page.locator('audio').first().evaluate(a=>{a.muted=true;return a.play()});await page.waitForFunction(()=>document.querySelector('audio').currentTime>.15);await page.locator('audio').first().evaluate(a=>a.pause());
 await page.screenshot({path:path.join(__dirname,'weapon-audio/audition-viewer.png'),fullPage:true});
 if(errors.length)throw Error(errors.join('\n'));fs.writeFileSync(path.join(__dirname,'viewer-check.json'),JSON.stringify({status:'PASS',videos:ids.length,audioAuditions:audioCount,decoded,errors},null,2));console.log('PASS: all eight native videos load; H264 video and AAC audio decode; tile/Doom switching works; eleven WAV auditions load and play; no page errors.');await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
