class ApplicationController < ActionController::Base
  protect_from_forgery with: :null_session
  before_action :authenticate_user!

  private

  def authenticate_user!
    token = request.headers['Authorization'].to_s.delete_prefix('Bearer ')
    payload = JWT.decode(token, ENV.fetch('JWT_SECRET'), true, algorithm: 'HS256').first
    @current_user = User.find(payload['sub'])
  rescue JWT::DecodeError, ActiveRecord::RecordNotFound
    render json: { error: 'login required' }, status: :unauthorized
  end

  def current_user
    @current_user
  end
end
