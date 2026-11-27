module Api
  class SessionsController < ApplicationController
    skip_before_action :authenticate_user!

    RETURN_PATHS = ['/', '/orders', '/account'].freeze

    def create
      user = User.authenticate(params[:email], params[:password])
      return render json: { error: 'invalid credentials' }, status: :unauthorized if user.nil?

      token = JWT.encode({ sub: user.id, exp: 1.hour.from_now.to_i }, ENV.fetch('JWT_SECRET'), 'HS256')
      render json: { token: token }
    end

    def reset
      token = rand(100_000..999_999).to_s
      User.where(email: params[:email]).update_all(reset_token: token)
      render json: { sent: true }
    end

    def destroy
      redirect_to params[:next].presence || '/', allow_other_host: true
    end

    def return_to
      target = RETURN_PATHS.include?(params[:to]) ? params[:to] : '/'
      redirect_to target
    end
  end
end
