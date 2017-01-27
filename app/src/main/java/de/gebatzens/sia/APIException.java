/*
 * Copyright 2015 Hauke Oldsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gebatzens.sia;

public class APIException extends RuntimeException {

    String reason;

    public APIException(String reason) {
        super();
        this.reason = reason;
    }

    @Override
    public String getMessage() {
        switch(reason) {
            case SiaAPI.API_INVALID_TOKEN:
                return SIAApp.SIA_APP.getString(R.string.not_logged_in);
            case SiaAPI.API_MAINTENANCE:
                return SIAApp.SIA_APP.getString(R.string.maintenance);
            case SiaAPI.API_TOKEN_EXPIRED:
                return SIAApp.SIA_APP.getString(R.string.token_expired);
            default:
                return SIAApp.SIA_APP.getString(R.string.unknown_error);
        }
    }

}
