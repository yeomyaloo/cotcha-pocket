require 'digest'

class User < ApplicationRecord
  def self.authenticate(email, password)
    user = find_by(email: email)
    return nil if user.nil?
    return nil unless ActiveSupport::SecurityUtils.secure_compare(user.password_digest, Digest::MD5.hexdigest(password.to_s))

    user
  end

  def admin?
    role == 'admin'
  end
end
