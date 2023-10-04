## [1.0.1](https://github.com/cake-lier/lambdas-as-a-service/compare/v1.0.0...v1.0.1) (2023-10-04)


### Bug Fixes

* add missing null value case in DSL, add missing test for tuple in DSL ([b01f917](https://github.com/cake-lier/lambdas-as-a-service/commit/b01f9170b76a00ed0567bacca7051c543a76915e))
* add missing reception of failures in execution request ([7d95b66](https://github.com/cake-lier/lambdas-as-a-service/commit/7d95b66112dcb27f29bd5888890e077f3267d356))
* fix package for tuple space service main class ([374f83e](https://github.com/cake-lier/lambdas-as-a-service/commit/374f83ea25d5b6b50e6049fe4583212a17eb4c22))
* fix package scope for server traits and object, apply same refactoring to tests ([d5a1467](https://github.com/cake-lier/lambdas-as-a-service/commit/d5a1467d5ff2a70093074b9aa9e2364b593dca23))
* uniform package structure to adhere to web service suggested structure ([336a4ab](https://github.com/cake-lier/lambdas-as-a-service/commit/336a4ab4d6515d6dbecc7159a14cee23896a15ee))

# 1.0.0 (2023-09-08)


### Bug Fixes

* add file removal on failure ([e61e2be](https://github.com/cake-lier/lambdas-as-a-service/commit/e61e2be8e23d58d0ea6aed97bba83cb9abda0cb9))
* add longer timeouts for websocket subscription due to long wait in compose, reduce exponential backoff max delay ([684e640](https://github.com/cake-lier/lambdas-as-a-service/commit/684e640cd747d105e68937d907cee5f26c7888e5))
* add longer timeouts for websocket subscription due to long wait in compose, remove prematerialization of sources ([d0301ce](https://github.com/cake-lier/lambdas-as-a-service/commit/d0301ce7c15703f0601cae6d4fdb0bb616b79ddd))
* add missing service path for file download ([8d6d579](https://github.com/cake-lier/lambdas-as-a-service/commit/8d6d5793069d6dcbbef007cac6fbe25aed340170))
* add password hashing, limit size of input in accord to database column size, return informative error when username is duplicated ([43c02a8](https://github.com/cake-lier/lambdas-as-a-service/commit/43c02a89a97f3f336ef0b600d330bb8a0ef3bfe0))
* add service paths for web UI ([06de5d0](https://github.com/cake-lier/lambdas-as-a-service/commit/06de5d0a5e606e8c04d7204759851100b5384f54))
* add tuple space closing after system termination in worker ([0f70ac2](https://github.com/cake-lier/lambdas-as-a-service/commit/0f70ac24835100b9a5f04cb8e8f94d463ebc6af2))
* correct favicon for UI ([3c52777](https://github.com/cake-lier/lambdas-as-a-service/commit/3c52777156f39de40f4eef429806995f12286eea))
* fix bug where websockets automatically close after a while ([6f0f93e](https://github.com/cake-lier/lambdas-as-a-service/commit/6f0f93ec0d505abb21b53cf8d29a33c43f2a54f8))
* fix bugs about repeated actor main calls, tuple space message buffer too small, in operations split to work for newly deployed executables, executables not working due to missing extension ([a51e53c](https://github.com/cake-lier/lambdas-as-a-service/commit/a51e53ca51ff5294baaf45fc4f0efb5747ec632b))
* readd missing .dockerignore file ([5c40d1a](https://github.com/cake-lier/lambdas-as-a-service/commit/5c40d1a658fc479555ba3c4a2acb814a2cfde397))
* remove useless parameter username in logout request, refactor file deletion ([a142307](https://github.com/cake-lier/lambdas-as-a-service/commit/a1423075332f81412c78f971d4a735e7b3b273e3))
* remove username from logout request, use socket id for keeping session in browser, add timeout to session, remove prematerialization of source in ws stream ([aee51cd](https://github.com/cake-lier/lambdas-as-a-service/commit/aee51cd56c57357ba01e4461dc997b363f5d891d))
* simplify dialog opening and closing ([09ae1f2](https://github.com/cake-lier/lambdas-as-a-service/commit/09ae1f2a922b8f2d1c6a3c4a7a7ce87708e49815))
* update field name in login form to be username and not email ([c29266d](https://github.com/cake-lier/lambdas-as-a-service/commit/c29266d4706661e1ae58705c41ad848b025a6792))


### Features

* add api agent for master webservice ([f812a43](https://github.com/cake-lier/lambdas-as-a-service/commit/f812a43b84679f6c312714e168635834d7f5ab83))
* add example docker-compose configuration ([b816bc7](https://github.com/cake-lier/lambdas-as-a-service/commit/b816bc776d45763dcfeb2e296a884e38e5ac3b97))
* add implementation for worker agent ([b733a36](https://github.com/cake-lier/lambdas-as-a-service/commit/b733a36550ce084f98d5db4ecd788c9cec40675d))
* add main file for worker service launch ([bcaf175](https://github.com/cake-lier/lambdas-as-a-service/commit/bcaf175b4c380e59696ff9f2e6043e94b005e07b))
* add main for starting up master service, dockerize worker and master ([66fea4e](https://github.com/cake-lier/lambdas-as-a-service/commit/66fea4e75dfbff599e87d592ecb15e0139552a84))
* add model and storage for master service ([262b117](https://github.com/cake-lier/lambdas-as-a-service/commit/262b117ca5121c95227c6cc44f2cd2f5f7b980ed))
* add React UI implementation ([ef828ff](https://github.com/cake-lier/lambdas-as-a-service/commit/ef828ffb5f54292494f75c692ac5f8e3ed9c3aab))
* add runner agent implementation ([0536206](https://github.com/cake-lier/lambdas-as-a-service/commit/0536206171029a7beabed72b3073b2296b7ddfd6))
* add webservice controller implementation and test ([358846b](https://github.com/cake-lier/lambdas-as-a-service/commit/358846b0d193616ad5209327d64a045fef6b4233))

# [1.0.0-beta.4](https://github.com/cake-lier/lambdas-as-a-service/compare/v1.0.0-beta.3...v1.0.0-beta.4) (2023-09-07)


### Bug Fixes

* add file removal on failure ([e61e2be](https://github.com/cake-lier/lambdas-as-a-service/commit/e61e2be8e23d58d0ea6aed97bba83cb9abda0cb9))
* add longer timeouts for websocket subscription due to long wait in compose, reduce exponential backoff max delay ([684e640](https://github.com/cake-lier/lambdas-as-a-service/commit/684e640cd747d105e68937d907cee5f26c7888e5))
* add longer timeouts for websocket subscription due to long wait in compose, remove prematerialization of sources ([d0301ce](https://github.com/cake-lier/lambdas-as-a-service/commit/d0301ce7c15703f0601cae6d4fdb0bb616b79ddd))
* add password hashing, limit size of input in accord to database column size, return informative error when username is duplicated ([43c02a8](https://github.com/cake-lier/lambdas-as-a-service/commit/43c02a89a97f3f336ef0b600d330bb8a0ef3bfe0))
* add service paths for web UI ([06de5d0](https://github.com/cake-lier/lambdas-as-a-service/commit/06de5d0a5e606e8c04d7204759851100b5384f54))
* correct favicon for UI ([3c52777](https://github.com/cake-lier/lambdas-as-a-service/commit/3c52777156f39de40f4eef429806995f12286eea))
* fix bug where websockets automatically close after a while ([6f0f93e](https://github.com/cake-lier/lambdas-as-a-service/commit/6f0f93ec0d505abb21b53cf8d29a33c43f2a54f8))
* fix bugs about repeated actor main calls, tuple space message buffer too small, in operations split to work for newly deployed executables, executables not working due to missing extension ([a51e53c](https://github.com/cake-lier/lambdas-as-a-service/commit/a51e53ca51ff5294baaf45fc4f0efb5747ec632b))
* remove useless parameter username in logout request, refactor file deletion ([a142307](https://github.com/cake-lier/lambdas-as-a-service/commit/a1423075332f81412c78f971d4a735e7b3b273e3))
* remove username from logout request, use socket id for keeping session in browser, add timeout to session, remove prematerialization of source in ws stream ([aee51cd](https://github.com/cake-lier/lambdas-as-a-service/commit/aee51cd56c57357ba01e4461dc997b363f5d891d))
* simplify dialog opening and closing ([09ae1f2](https://github.com/cake-lier/lambdas-as-a-service/commit/09ae1f2a922b8f2d1c6a3c4a7a7ce87708e49815))
* update field name in login form to be username and not email ([c29266d](https://github.com/cake-lier/lambdas-as-a-service/commit/c29266d4706661e1ae58705c41ad848b025a6792))


### Features

* add React UI implementation ([ef828ff](https://github.com/cake-lier/lambdas-as-a-service/commit/ef828ffb5f54292494f75c692ac5f8e3ed9c3aab))

# [1.0.0-beta.3](https://github.com/cake-lier/lambdas-as-a-service/compare/v1.0.0-beta.2...v1.0.0-beta.3) (2023-08-24)


### Bug Fixes

* add missing service path for file download ([8d6d579](https://github.com/cake-lier/lambdas-as-a-service/commit/8d6d5793069d6dcbbef007cac6fbe25aed340170))
* add tuple space closing after system termination in worker ([0f70ac2](https://github.com/cake-lier/lambdas-as-a-service/commit/0f70ac24835100b9a5f04cb8e8f94d463ebc6af2))


### Features

* add api agent for master webservice ([f812a43](https://github.com/cake-lier/lambdas-as-a-service/commit/f812a43b84679f6c312714e168635834d7f5ab83))
* add example docker-compose configuration ([b816bc7](https://github.com/cake-lier/lambdas-as-a-service/commit/b816bc776d45763dcfeb2e296a884e38e5ac3b97))
* add main for starting up master service, dockerize worker and master ([66fea4e](https://github.com/cake-lier/lambdas-as-a-service/commit/66fea4e75dfbff599e87d592ecb15e0139552a84))
* add model and storage for master service ([262b117](https://github.com/cake-lier/lambdas-as-a-service/commit/262b117ca5121c95227c6cc44f2cd2f5f7b980ed))
* add webservice controller implementation and test ([358846b](https://github.com/cake-lier/lambdas-as-a-service/commit/358846b0d193616ad5209327d64a045fef6b4233))

# [1.0.0-beta.2](https://github.com/cake-lier/lambdas-as-a-service/compare/v1.0.0-beta.1...v1.0.0-beta.2) (2023-08-08)


### Features

* add implementation for worker agent ([b733a36](https://github.com/cake-lier/lambdas-as-a-service/commit/b733a36550ce084f98d5db4ecd788c9cec40675d))
* add main file for worker service launch ([bcaf175](https://github.com/cake-lier/lambdas-as-a-service/commit/bcaf175b4c380e59696ff9f2e6043e94b005e07b))
* add runner agent implementation ([0536206](https://github.com/cake-lier/lambdas-as-a-service/commit/0536206171029a7beabed72b3073b2296b7ddfd6))

# 1.0.0-beta.1 (2023-08-03)


### Bug Fixes

* readd missing .dockerignore file ([5c40d1a](https://github.com/cake-lier/lambdas-as-a-service/commit/5c40d1a658fc479555ba3c4a2acb814a2cfde397))
