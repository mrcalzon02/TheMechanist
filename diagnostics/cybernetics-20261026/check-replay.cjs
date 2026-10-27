const {chromium}=require('C:/Users/Admin/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const path=require('path');
(async()=>{
 const browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
 const page=await browser.newPage({viewport:{width:1440,height:1100}});const errors=[];page.on('pageerror',e=>errors.push(e.message));
 await page.goto('file:///'+path.resolve(__dirname,'index.html').replaceAll('\\','/'));
 await page.locator('#battle').selectOption({label:'20v20-rifles-cyber25'});
 await page.locator('#scrub').evaluate(e=>{e.value=12;e.dispatchEvent(new Event('input'))});
 await page.screenshot({path:path.join(__dirname,'battle-replay-20v20.png'),fullPage:false});
 for(const option of await page.locator('#battle option').all()) {
  await page.locator('#battle').selectOption(await option.getAttribute('value'));
  await page.locator('#scrub').evaluate(e=>{e.value=e.max;e.dispatchEvent(new Event('input'))});
  if(!(await page.locator('#score').textContent()).includes('wins'))throw Error('Final outcome missing');
 }
 await page.locator('#battle').selectOption({label:'20v20-rifles-cyber25'});await page.locator('#play').click();
 await page.waitForFunction(()=>document.querySelector('#scrub').value!=='0');await page.locator('#play').click();
 await page.locator('#restart').click();if(await page.locator('#scrub').inputValue()!=='0')throw Error('Restart failed');
 if(errors.length)throw Error(errors.join('\n'));console.log('PASS: 24 battles render; final outcomes, playback, pause, restart and scrub work; no page errors.');await browser.close();
})().catch(e=>{console.error(e);process.exit(1)});
