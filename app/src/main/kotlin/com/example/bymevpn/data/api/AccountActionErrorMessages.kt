package com.example.bymevpn.data.api

/** Short user-facing explanations for account actions, without exposing API internals. */
object AccountActionErrorMessages {
    fun googleSignIn(error: Throwable?, isRussian: Boolean): String {
        val apiError = error as? ApiError
        val message = when {
            apiError?.code == "USER_CANCELLED" -> ""
            apiError?.code == "NETWORK_ERROR" -> if (isRussian) {
                "Нет соединения. Проверьте интернет и попробуйте войти снова."
            } else {
                "No connection. Check your internet and try signing in again."
            }
            apiError?.httpStatusCode == 503 || apiError?.code == "GOOGLE_AUTH_UNAVAILABLE" -> if (isRussian) {
                "Вход через Google временно недоступен. Попробуйте позже."
            } else {
                "Google sign-in is temporarily unavailable. Please try again later."
            }
            apiError?.httpStatusCode == 401 -> if (isRussian) {
                "Google выбрал аккаунт, но вход не завершился. Попробуйте ещё раз."
            } else {
                "Google selected the account, but sign-in did not finish. Please try again."
            }
            else -> if (isRussian) {
                "Не удалось войти через Google. Попробуйте ещё раз или войдите по email."
            } else {
                "Could not sign in with Google. Try again or sign in with email."
            }
        }
        return appendSupportCode(message, apiError)
    }

    fun telegramSignInStart(error: Throwable?, isRussian: Boolean): String {
        val apiError = error as? ApiError
        val message = when {
            apiError?.code == "NETWORK_ERROR" -> if (isRussian) {
                "Нет соединения. Проверьте интернет и попробуйте снова."
            } else {
                "No connection. Check your internet and try again."
            }
            apiError?.httpStatusCode == 401 -> if (isRussian) {
                "Сеанс входа истёк. Начните вход заново."
            } else {
                "Your sign-in session expired. Please start again."
            }
            apiError?.httpStatusCode?.let { it in 500..599 } == true -> if (isRussian) {
                "Сервис входа временно недоступен. Попробуйте позже."
            } else {
                "Sign-in is temporarily unavailable. Please try again later."
            }
            else -> if (isRussian) {
                "Не удалось начать вход через Telegram. Попробуйте ещё раз."
            } else {
                "Could not start Telegram sign-in. Please try again."
            }
        }
        return appendSupportCode(message, apiError)
    }

    fun activateTrial(error: Throwable?, isRussian: Boolean): String {
        val apiError = error as? ApiError
        val message = when {
            apiError?.code == "NETWORK_ERROR" -> if (isRussian) {
                "Нет соединения. Проверьте интернет и попробуйте активировать пробный период снова."
            } else {
                "No connection. Check your internet and try activating the trial again."
            }
            apiError?.code == "UNAUTHORIZED" || apiError?.httpStatusCode == 401 -> if (isRussian) {
                "Войдите в аккаунт заново, затем повторите попытку."
            } else {
                "Sign in again, then retry."
            }
            apiError?.httpStatusCode == 409 -> if (isRussian) {
                "Пробный период уже использован или подключён. Обновите статус в аккаунте."
            } else {
                "The trial was already used or activated. Refresh your account status."
            }
            apiError?.httpStatusCode == 422 -> if (isRussian) {
                "Не удалось подтвердить это устройство. Перезапустите приложение и попробуйте снова."
            } else {
                "This device could not be confirmed. Restart the app and try again."
            }
            apiError?.httpStatusCode?.let { it in 500..599 } == true -> if (isRussian) {
                "Не удалось подключить пробный период. Попробуйте позже."
            } else {
                "Could not activate the trial. Please try again later."
            }
            else -> if (isRussian) {
                "Не удалось активировать пробный период. Попробуйте ещё раз."
            } else {
                "Could not activate the trial. Please try again."
            }
        }
        return appendSupportCode(message, apiError)
    }

    fun deleteDevice(error: Throwable?, isRussian: Boolean): String {
        val apiError = error as? ApiError
        return when {
            apiError?.code == "UNAUTHORIZED" || apiError?.httpStatusCode == 401 -> if (isRussian) {
                "Сеанс завершился. Войдите в аккаунт и повторите удаление."
            } else {
                "Your session expired. Sign in again and retry."
            }
            apiError?.httpStatusCode == 404 -> if (isRussian) {
                "Устройство уже удалено или больше не найдено. Обновите список."
            } else {
                "This device was already removed or could not be found. Refresh the list."
            }
            apiError?.code == "NETWORK_ERROR" -> if (isRussian) {
                "Нет соединения с сервером. Проверьте интернет и попробуйте снова."
            } else {
                "Could not reach the server. Check your internet and try again."
            }
            apiError?.httpStatusCode?.let { it in 500..599 } == true -> if (isRussian) {
                "Сервер не смог удалить устройство. Попробуйте ещё раз позже."
            } else {
                "The server could not remove this device. Please try again later."
            }
            else -> if (isRussian) {
                "Не удалось удалить устройство. Обновите список и попробуйте снова."
            } else {
                "Could not remove this device. Refresh the list and try again."
            }
        }
    }

    private fun appendSupportCode(message: String, error: ApiError?): String {
        if (message.isBlank()) return message
        val correlationId = error?.correlationId ?: return message
        return "$message (код: $correlationId)"
    }
}
