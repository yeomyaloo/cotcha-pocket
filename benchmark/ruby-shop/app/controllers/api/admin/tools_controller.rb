require 'erb'

module Api
  module Admin
    class ToolsController < ApplicationController
      before_action :require_admin

      REPORT_TABLES = %w[orders products].freeze

      def export
        ok = system("tar czf /tmp/export.tgz /var/shop/exports/#{params[:dir]}")
        return render json: { error: 'export failed' }, status: :internal_server_error unless ok

        send_file '/tmp/export.tgz'
      end

      def report
        table = params[:table].to_s
        return render json: { error: 'unknown table' }, status: :bad_request unless REPORT_TABLES.include?(table)

        system('pg_dump', '--data-only', "--table=#{table}", 'shop', out: '/tmp/report.sql')
        send_file '/tmp/report.sql', type: 'text/plain'
      end

      def preview
        html = ERB.new(params[:template].to_s).result(binding)
        render html: html
      end

      private

      def require_admin
        render json: { error: 'admin only' }, status: :forbidden unless current_user&.admin?
      end
    end
  end
end
