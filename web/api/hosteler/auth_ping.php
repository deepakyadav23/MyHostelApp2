<?php
//include 'res/logger.php';
$GLOBALS['timeout']['connection']=15;
$GLOBALS['timeout']['operation']=20;
$GLOBALS['result']['error']=false;
$GLOBALS['result']['errorCode']="";
$GLOBALS['result']['errorMessage']="";
$GLOBALS['result']['outing_type']="null";
function dataCleaner($dirty){
	$clean=htmlspecialchars(strip_tags(addslashes(trim(filter_var($dirty, FILTER_SANITIZE_STRING)))));
	return $clean;
}

function checkToken(){
	require $_SERVER['DOCUMENT_ROOT']."/api/hosteler/cnxn/conxhosteler.php";
	$stmtCheckToken->execute();
	$stmtCheckToken->setFetchMode(PDO::FETCH_ASSOC);
	$result=$stmtCheckToken->fetchAll();
	$con=null;
	if (count($result)>0) {
		$GLOBALS['result']['access_token']=$result[0]['access_token'];
		return TRUE;
	}else{
		$GLOBALS['result']['error']=true;
		$GLOBALS['result']['errorCode']="IAT";
		$GLOBALS['result']['errorMessage']="Invalid AccessToken, what the fuck?";
		return FALSE;
	}
}



function checkHeaders(){
	$GLOBALS['headers']=getallheaders();
	if (isset($GLOBALS['headers']['reqty'])&&!empty($GLOBALS['headers']['reqty'])){
		if ($GLOBALS['headers']['reqty']=='auth_ping') {
			return true;
		}
	}
	return false;
}


function getArgs(){
	$argsMissing=false;
	if (isset($_POST['user_id'])&&!empty($_POST['user_id'])&&isset($_POST['access_token'])&&!empty($_POST['access_token'])) {
		$GLOBALS['args']['user_id']=dataCleaner($_POST['user_id']);
		$GLOBALS['args']['access_token']=dataCleaner($_POST['access_token']);
	}else{
		$argsMissing=true;
	}
	return !$argsMissing;
}


if (checkHeaders()) {
	if (getArgs()) {
		if (checkToken()) {
			$GLOBALS['result']['auth_ping']='success';
			
		}else{

			$GLOBALS['result']['auth_ping']='failed';
		}
	}else{
		$GLOBALS['result']['error']=true;
		$GLOBALS['result']['errorCode']="IA";
		$GLOBALS['result']['errorMessage']="Invalid Argument types";
	}
}else{

	$GLOBALS['result']['error']=true;
	$GLOBALS['result']['errorCode']="IH";
	$GLOBALS['result']['errorMessage']="Invalid Headers";
}


echo json_encode($GLOBALS['result']);