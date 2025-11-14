module.exports = {
  setAuthToken: setAuthToken,
  logResponse: logResponse
};

function setAuthToken(requestParams, context, ee, next) {
  if (context.vars.authToken) {
    requestParams.headers = requestParams.headers || {};
    requestParams.headers['Authorization'] = `Bearer ${context.vars.authToken}`;
  }
  return next();
}

function logResponse(requestParams, response, context, ee, next) {
  if (response.statusCode >= 400) {
    console.log(`Error ${response.statusCode}: ${response.body}`);
  }
  return next();
}
